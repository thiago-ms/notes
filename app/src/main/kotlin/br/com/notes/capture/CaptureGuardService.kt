package br.com.notes.capture

import android.app.Service
import android.content.Intent
import android.os.IBinder
import br.com.notes.core.data.repo.AutoCaptureState

/**
 * Serviço leve iniciado pela Activity só para detectar quando o app é **fechado**
 * (removido dos recentes): aí desliga a auto captura. Assim o estado não "vaza" para
 * depois de fechar o app, mesmo com o serviço de acessibilidade mantendo o processo
 * vivo. (Ao matar o processo, o flag em memória zera de qualquer forma.)
 */
class CaptureGuardService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        AutoCaptureState.definir(false)
        runCatching { stopService(Intent(this, FloatingButtonService::class.java)) }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
