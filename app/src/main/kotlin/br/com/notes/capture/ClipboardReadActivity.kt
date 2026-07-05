package br.com.notes.capture

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Activity transparente e instantânea. Existe só para, ao ser trazida ao primeiro
 * plano pelo toque no botão flutuante, **ter foco** e assim conseguir ler o clipboard
 * (o que um app em background não pode). Roda numa task própria (taskAffinity="") e se
 * fecha com finishAndRemoveTask() — assim o sistema volta para o app anterior (ex.:
 * Chrome) em vez de trazer o Notas para a frente. A captura e o toast rodam no
 * [CaptureRunner], desacoplados desta Activity que fecha na hora.
 */
class ClipboardReadActivity : ComponentActivity() {

    private var feito = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || feito) return
        feito = true
        CaptureRunner.capturar(applicationContext, lerClipboard())
        finishAndRemoveTask()
        overridePendingTransition(0, 0)
    }

    private fun lerClipboard(): String? {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return null
        val texto = (0 until clip.itemCount)
            .mapNotNull { clip.getItemAt(it).coerceToText(this)?.toString() }
            .joinToString("\n")
            .trim()
        return texto.ifEmpty { null }
    }
}
