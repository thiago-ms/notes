package br.com.notes.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ServiceCompat
import br.com.notes.core.data.repo.CapturePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Serviço em primeiro plano (notificação fixa) que hospeda o botão flutuante do modo
 * de auto captura sem acessibilidade. O botão fica **sempre ativo**: ao tocar, abre a
 * [ClipboardReadActivity], que — por vir ao primeiro plano — lê o clipboard e captura.
 * Acima do botão há um **contador** de capturas da sessão (feedback no lugar do toast).
 * Arrastável; a posição é persistida.
 */
class FloatingButtonService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var wm: WindowManager
    private var container: LinearLayout? = null
    private var badge: TextView? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        iniciarForeground()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        CaptureCounter.zerar()
        mostrarBotao()
        scope.launch {
            CaptureCounter.count.collect { n -> atualizarBadge(n) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun iniciarForeground() {
        val canalId = "notes_captura_flutuante"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalId,
                "Auto captura (botão flutuante)",
                NotificationManager.IMPORTANCE_LOW,
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(canal)
        }
        val notif: Notification = Notification.Builder(this, canalId)
            .setContentTitle("Notas — captura por botão")
            .setContentText("Toque no botão flutuante para colar no bloco atual.")
            .setSmallIcon(applicationInfo.icon)
            .setOngoing(true)
            .build()

        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, notif, tipo)
    }

    private fun mostrarBotao() {
        val (x, y) = CapturePrefs.getPos(this)
        val dens = resources.displayMetrics.density
        val tamanho = (56 * dens).roundToInt()

        val numero = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val padH = (6 * dens).roundToInt()
            val padV = (1 * dens).roundToInt()
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(Color.parseColor("#CC000000"))
            }
            visibility = View.GONE
        }

        val botao = TextView(this).apply {
            text = "📋"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#5B4BE8"))
            }
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(
                numero,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = (4 * dens).roundToInt() },
            )
            addView(botao, LinearLayout.LayoutParams(tamanho, tamanho))
            setOnTouchListener(criarTouchListener())
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoOverlay(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        container = box
        badge = numero
        atualizarBadge(CaptureCounter.count.value)
        wm.addView(box, params)
    }

    private fun atualizarBadge(n: Int) {
        badge?.apply {
            if (n > 0) {
                text = n.toString()
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun criarTouchListener(): View.OnTouchListener {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var initX = 0
        var initY = 0
        var downRawX = 0f
        var downRawY = 0f
        var arrastou = false

        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x
                    initY = params.y
                    downRawX = event.rawX
                    downRawY = event.rawY
                    arrastou = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).roundToInt()
                    val dy = (event.rawY - downRawY).roundToInt()
                    if (abs(dx) > slop || abs(dy) > slop) arrastou = true
                    params.x = initX + dx
                    params.y = initY + dy
                    container?.let { wm.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (arrastou) {
                        CapturePrefs.setPos(this, params.x, params.y)
                    } else {
                        aoTocar()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun aoTocar() {
        startActivity(
            Intent(this, ClipboardReadActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                ),
        )
    }

    private fun tipoOverlay(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    override fun onDestroy() {
        container?.let { runCatching { wm.removeView(it) } }
        container = null
        badge = null
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val NOTIF_ID = 42
    }
}
