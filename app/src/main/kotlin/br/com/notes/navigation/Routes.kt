package br.com.notes.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rotas de navegação. As duas abas (Nota atual, Blocos) ficam no bottom navigation;
 * Configurações é um destino empilhado (push), aberto pela engrenagem.
 */
object Routes {
    const val NOTA = "nota"
    const val BLOCOS = "blocos"
    const val CONFIG = "config"
}

/** Aba do bottom navigation. A ordem define a posição na barra. */
enum class TabDestination(val route: String, val label: String, val icon: ImageVector) {
    NOTA(Routes.NOTA, "Nota", Icons.Filled.Edit),
    BLOCOS(Routes.BLOCOS, "Blocos", Icons.AutoMirrored.Filled.List),
}

val TAB_ROUTES: Set<String> = TabDestination.entries.map { it.route }.toSet()
