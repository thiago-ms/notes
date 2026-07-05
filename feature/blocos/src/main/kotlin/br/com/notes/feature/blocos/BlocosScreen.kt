package br.com.notes.feature.blocos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.notes.core.data.model.Bloco
import br.com.notes.core.data.repo.NotesRepository
import br.com.notes.core.ui.component.EmptyState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FORMATO_DATA = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

/**
 * Aba "Blocos" — lista de todos os blocos salvos (inclui o atual), ordenada pela
 * última atualização. Tocar abre o bloco (vira o atual). Cada item permite renomear
 * e apagar. A engrenagem abre as Configurações (auto captura + backup).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocosScreen(
    onAbrir: (Long) -> Unit,
    onOpenConfig: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { NotesRepository.get(context) }
    val scope = rememberCoroutineScope()

    val blocos by repo.observarBlocos().collectAsStateWithLifecycle(emptyList())

    var renomeando by remember { mutableStateOf<Bloco?>(null) }
    var apagando by remember { mutableStateOf<Bloco?>(null) }

    renomeando?.let { alvo ->
        var texto by remember(alvo.id) { mutableStateOf(alvo.nome) }
        AlertDialog(
            onDismissRequest = { renomeando = null },
            title = { Text("Renomear bloco") },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    singleLine = true,
                    label = { Text("Nome") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val novo = texto.trim()
                    renomeando = null
                    if (novo.isNotEmpty()) scope.launch { repo.renomear(alvo.id, novo) }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { renomeando = null }) { Text("Cancelar") } },
        )
    }

    apagando?.let { alvo ->
        AlertDialog(
            onDismissRequest = { apagando = null },
            title = { Text("Apagar bloco?") },
            text = { Text("\"${alvo.nome}\" será removido. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    apagando = null
                    scope.launch { repo.remover(alvo) }
                }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { apagando = null }) { Text("Cancelar") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocos") },
                actions = {
                    IconButton(onClick = onOpenConfig) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (blocos.isEmpty()) {
            EmptyState(
                mensagem = "Nenhum bloco ainda. Toque em + para criar sua primeira nota.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        ) {
            items(blocos, key = { it.id }) { bloco ->
                BlocoRow(
                    bloco = bloco,
                    onAbrir = { onAbrir(bloco.id) },
                    onRenomear = { renomeando = bloco },
                    onApagar = { apagando = bloco },
                )
            }
        }
    }
}

@Composable
private fun BlocoRow(
    bloco: Bloco,
    onAbrir: () -> Unit,
    onRenomear: () -> Unit,
    onApagar: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrir),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    bloco.nome,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Atualizado ${FORMATO_DATA.format(Date(bloco.dataAtualizacao))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRenomear) {
                Icon(Icons.Filled.Edit, contentDescription = "Renomear")
            }
            IconButton(onClick = onApagar) {
                Icon(Icons.Filled.Delete, contentDescription = "Apagar")
            }
        }
    }
}
