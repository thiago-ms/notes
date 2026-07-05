package br.com.notes.feature.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Utilitários para o serviço de acessibilidade que habilita a auto captura em
 * segundo plano. Verifica sem referenciar a classe do :app (checa se algum serviço
 * habilitado pertence ao pacote do app).
 */
object AcessibilidadeUtil {

    fun servicoAtivo(context: Context): Boolean {
        val habilitados = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val pkg = context.packageName
        return habilitados.split(':').any { it.substringBefore('/') == pkg }
    }

    fun abrirAjustes(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
