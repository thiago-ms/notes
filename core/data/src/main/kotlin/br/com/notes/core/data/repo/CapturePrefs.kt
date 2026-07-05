package br.com.notes.core.data.repo

import android.content.Context

/** Modo de auto captura escolhido pelo usuário. */
enum class CaptureMode {
    /** Lê o clipboard em segundo plano automaticamente (requer acessibilidade). */
    ACESSIBILIDADE,

    /** Botão flutuante: você toca para colar (requer "desenhar sobre outros apps"). */
    FLUTUANTE,
}

/** O que fazer ao tocar num link renderizado no bloco. */
enum class LinkAcao {
    /** Copia o link para o clipboard (padrão). */
    COPIAR,

    /** Abre o link no navegador. */
    ABRIR,
}

/**
 * Preferências da auto captura que **persistem** (diferente do liga/desliga, que é só
 * em memória): o modo escolhido e a última posição do botão flutuante.
 */
object CapturePrefs {
    private const val NOME = "notes_captura"
    private const val K_MODO = "modo"
    private const val K_X = "pos_x"
    private const val K_Y = "pos_y"
    private const val K_LINK = "link_acao"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NOME, Context.MODE_PRIVATE)

    fun getModo(context: Context): CaptureMode =
        runCatching { CaptureMode.valueOf(prefs(context).getString(K_MODO, null) ?: "") }
            .getOrDefault(CaptureMode.ACESSIBILIDADE)

    fun setModo(context: Context, modo: CaptureMode) {
        prefs(context).edit().putString(K_MODO, modo.name).apply()
    }

    /** Posição (x, y) do botão flutuante em px, a partir do canto superior esquerdo. */
    fun getPos(context: Context): Pair<Int, Int> =
        prefs(context).getInt(K_X, 24) to prefs(context).getInt(K_Y, 240)

    fun setPos(context: Context, x: Int, y: Int) {
        prefs(context).edit().putInt(K_X, x).putInt(K_Y, y).apply()
    }

    fun getLinkAcao(context: Context): LinkAcao =
        runCatching { LinkAcao.valueOf(prefs(context).getString(K_LINK, null) ?: "") }
            .getOrDefault(LinkAcao.COPIAR)

    fun setLinkAcao(context: Context, acao: LinkAcao) {
        prefs(context).edit().putString(K_LINK, acao.name).apply()
    }
}
