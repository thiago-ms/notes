package br.com.notes.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta base (índigo/violeta). Ajuste as cores ao criar um app real.
private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BE8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3DEFF),
    onPrimaryContainer = Color(0xFF17004A),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFFE8A13B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC7BFFF),
    onPrimary = Color(0xFF2A1A6B),
    primaryContainer = Color(0xFF43349E),
    onPrimaryContainer = Color(0xFFE3DEFF),
    secondary = Color(0xFFCBC2DB),
    tertiary = Color(0xFFF3C27A),
)

/** Tema único do app, compartilhado por todos os módulos de feature. */
@Composable
fun NotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
