package br.com.notes.core.data.repo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado da auto captura — **apenas em memória**, de propósito. Não é persistido:
 * ao fechar o app (morte do processo) o flag volta a `false`, exatamente como pedido
 * ("só funciona com o app aberto; ao fechar, para"). A UI liga/desliga aqui e o
 * capturador do :app observa este flag enquanto o app está em primeiro plano.
 */
object AutoCaptureState {
    private val _ativo = MutableStateFlow(false)
    val ativo: StateFlow<Boolean> = _ativo

    fun definir(ativo: Boolean) {
        _ativo.value = ativo
    }

    fun alternar() {
        _ativo.value = !_ativo.value
    }
}
