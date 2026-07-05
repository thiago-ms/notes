package br.com.notes.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contador de capturas da sessão do botão flutuante (em memória). Incrementado a cada
 * captura bem-sucedida; exibido como um numerozinho acima do botão. Zera quando o
 * serviço do botão (re)inicia.
 */
object CaptureCounter {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count

    fun incrementar() {
        _count.value += 1
    }

    fun zerar() {
        _count.value = 0
    }
}
