package br.com.notes.feature.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Controla o serviço do botão flutuante sem referenciar a classe do :app (usa o nome
 * do componente). Também expõe o check e o pedido da permissão de overlay.
 */
object FloatingControl {

    private const val SERVICO = "br.com.notes.capture.FloatingButtonService"

    private fun intentServico(context: Context) =
        Intent().setComponent(ComponentName(context.packageName, SERVICO))

    fun temPermissaoOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun intentPermissaoOverlay(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )

    fun iniciar(context: Context) {
        context.startForegroundService(intentServico(context))
    }

    fun parar(context: Context) {
        runCatching { context.stopService(intentServico(context)) }
    }
}
