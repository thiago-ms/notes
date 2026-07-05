package br.com.notes.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.notes.core.data.repo.NotesRepository
import br.com.notes.feature.blocos.BlocosScreen
import br.com.notes.feature.notepad.NotepadScreen
import br.com.notes.feature.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * Casca de navegação: bottom navigation com 2 abas (Nota atual, Blocos) + FAB central
 * para criar uma nota nova. A barra e o FAB só aparecem nas abas; Configurações é um
 * destino empilhado. Trocar de aba limpa a pilha da aba.
 */
@Composable
fun NotesApp() {
    val context = LocalContext.current
    val repo = remember { NotesRepository.get(context) }
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val emAba = currentRoute in TAB_ROUTES

    Scaffold(
        bottomBar = {
            if (emAba) {
                NavigationBar {
                    TabDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.trocarAba(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (emAba) {
                FloatingActionButton(onClick = {
                    scope.launch {
                        repo.criarBloco()
                        navController.trocarAba(Routes.NOTA)
                    }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nova nota")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.NOTA,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Routes.NOTA) {
                NotepadScreen(onOpenConfig = { navController.navigate(Routes.CONFIG) })
            }
            composable(Routes.BLOCOS) {
                BlocosScreen(
                    onAbrir = { id ->
                        repo.definirBlocoAtual(id)
                        navController.trocarAba(Routes.NOTA)
                    },
                    onOpenConfig = { navController.navigate(Routes.CONFIG) },
                )
            }
            composable(Routes.CONFIG) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Troca de aba resetando a pilha da aba. */
private fun NavHostController.trocarAba(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
