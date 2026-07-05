package br.com.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.notes.capture.CaptureGuardService
import br.com.notes.core.ui.theme.NotesTheme
import br.com.notes.navigation.NotesApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Guard: ao remover o app dos recentes, desliga a auto captura (estado não
        // persiste). A captura em si é feita pelo CaptureAccessibilityService.
        startService(Intent(this, CaptureGuardService::class.java))

        setContent {
            NotesTheme {
                NotesApp()
            }
        }
    }
}
