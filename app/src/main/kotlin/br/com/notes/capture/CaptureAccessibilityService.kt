package br.com.notes.capture

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import br.com.notes.core.data.repo.AutoCaptureState
import br.com.notes.core.data.repo.CaptureMode
import br.com.notes.core.data.repo.CapturePrefs
import br.com.notes.core.data.repo.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Serviço de acessibilidade cujo único papel é dar ao app o direito de ler o
 * clipboard **em segundo plano** (o app em background não consegue por conta própria
 * desde o Android 10; só o app em foco e o teclado/IME padrão conseguem). Enquanto
 * habilitado, ouve mudanças do clipboard e, se a auto captura estiver ligada, roteia
 * o texto copiado para o bloco atual (ou para o bloco de uma regra).
 */
class CaptureAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var clipboard: ClipboardManager? = null
    private val listener = ClipboardManager.OnPrimaryClipChangedListener { aoMudarClipboard() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard = cm
        cm.addPrimaryClipChangedListener(listener)
    }

    private fun aoMudarClipboard() {
        if (!AutoCaptureState.ativo.value) return
        // Só age no modo acessibilidade; no modo botão flutuante a captura é por toque.
        if (CapturePrefs.getModo(this) != CaptureMode.ACESSIBILIDADE) return
        val texto = lerClipboard() ?: return
        val repo = NotesRepository.get(applicationContext)
        scope.launch {
            val destino = repo.capturar(texto)
            if (destino != null) toastRapido("Capturado em \"$destino\"")
        }
    }

    private fun lerClipboard(): String? {
        val clip = clipboard?.primaryClip ?: return null
        val texto = (0 until clip.itemCount)
            .mapNotNull { clip.getItemAt(it).coerceToText(this)?.toString() }
            .joinToString("\n")
            .trim()
        return texto.ifEmpty { null }
    }

    /** Toast o mais discreto/rápido possível: curto e cancelado em ~700ms. */
    private fun toastRapido(msg: String) {
        val t = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
        t.show()
        Handler(Looper.getMainLooper()).postDelayed({ t.cancel() }, 700)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Não precisamos reagir a eventos de UI — usamos só o listener do clipboard.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        clipboard?.removePrimaryClipChangedListener(listener)
        scope.cancel()
        super.onDestroy()
    }
}
