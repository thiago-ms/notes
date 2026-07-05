package br.com.notes.capture

import android.content.Context
import br.com.notes.core.data.repo.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Executa a captura em um escopo de **app** (não preso a nenhuma Activity), para que a
 * gravação aconteça mesmo quando a [ClipboardReadActivity] se fecha imediatamente após
 * ler o clipboard. O feedback é o contador acima do botão flutuante ([CaptureCounter]),
 * não um toast (que era pouco visível em background).
 */
object CaptureRunner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun capturar(appContext: Context, texto: String?) {
        if (texto.isNullOrBlank()) return
        scope.launch {
            // forcar=false: se for o mesmo texto já capturado, não duplica.
            val destino = NotesRepository.get(appContext).capturar(texto, forcar = false)
            if (destino != null) CaptureCounter.incrementar()
        }
    }
}
