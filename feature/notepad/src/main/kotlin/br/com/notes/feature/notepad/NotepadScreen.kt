package br.com.notes.feature.notepad

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.notes.core.data.model.Bloco
import br.com.notes.core.data.repo.CapturePrefs
import br.com.notes.core.data.repo.LinkAcao
import br.com.notes.core.data.repo.NotesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FORMATO_DATA = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

/**
 * Aba "Nota" — o bloco atual. Nome e conteúdo são editáveis com autosave (debounce);
 * as datas de criação e última atualização ficam visíveis. Se uma captura automática
 * cair neste bloco enquanto ele está aberto, o conteúdo é atualizado ao vivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreen(onOpenConfig: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NotesRepository.get(context) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var editando by remember { mutableStateOf(true) }
    var apagando by remember { mutableStateOf(false) }
    var removendoDup by remember { mutableStateOf(false) }
    val corLink = MaterialTheme.colorScheme.primary

    // Ação ao tocar num link no modo visualização (config na engrenagem; padrão copiar).
    fun acaoLink(url: String) {
        val alvo = if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
        when (CapturePrefs.getLinkAcao(context)) {
            LinkAcao.COPIAR -> {
                clipboard.setText(AnnotatedString(alvo))
                Toast.makeText(context, "Link copiado", Toast.LENGTH_SHORT).show()
            }
            LinkAcao.ABRIR -> runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(alvo)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    val blocoAtualId by repo.blocoAtualId.collectAsStateWithLifecycle()

    // Garante que exista um bloco atual ao abrir a aba (cria um se a base estiver vazia).
    val garantido by produceState<Long?>(null) { value = repo.garantirBlocoAtual() }
    val idAtual = blocoAtualId ?: garantido

    val bloco: Bloco? by produceState<Bloco?>(null, idAtual) {
        value = null
        val id = idAtual ?: return@produceState
        repo.observarBloco(id).collect { value = it }
    }

    // Estados locais dos campos, ressincronizados quando muda o bloco ou quando o
    // conteúdo muda por fora (captura automática).
    var nome by remember(idAtual) { mutableStateOf("") }
    var conteudo by remember(idAtual) { mutableStateOf("") }

    LaunchedEffectSync(bloco?.nome) { novo -> if (novo != null && novo != nome) nome = novo }
    LaunchedEffectSync(bloco?.conteudo) { novo -> if (novo != null && novo != conteudo) conteudo = novo }

    // Autosave com debounce.
    androidx.compose.runtime.LaunchedEffect(nome, idAtual) {
        val id = idAtual ?: return@LaunchedEffect
        val atual = bloco ?: return@LaunchedEffect
        if (nome != atual.nome && nome.isNotBlank()) {
            delay(400)
            repo.renomear(id, nome)
        }
    }
    androidx.compose.runtime.LaunchedEffect(conteudo, idAtual) {
        val id = idAtual ?: return@LaunchedEffect
        val atual = bloco ?: return@LaunchedEffect
        if (conteudo != atual.conteudo) {
            delay(400)
            repo.atualizarConteudo(id, conteudo)
        }
    }

    if (removendoDup) {
        val (resultado, removidas) = remember(conteudo) { removerLinhasDuplicadas(conteudo) }
        AlertDialog(
            onDismissRequest = { removendoDup = false },
            title = { Text("Remover linhas duplicadas?") },
            text = {
                Text(
                    if (removidas == 0) {
                        "Nenhuma linha duplicada encontrada."
                    } else {
                        "$removidas linha(s) repetida(s) serão removidas, mantendo a " +
                            "primeira ocorrência de cada uma."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = removidas > 0,
                    onClick = {
                        removendoDup = false
                        conteudo = resultado
                        Toast.makeText(context, "Duplicadas removidas", Toast.LENGTH_SHORT).show()
                    },
                ) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { removendoDup = false }) { Text("Cancelar") }
            },
        )
    }

    if (apagando) {
        val alvo = bloco
        AlertDialog(
            onDismissRequest = { apagando = false },
            title = { Text("Apagar bloco?") },
            text = {
                Text(
                    "\"${alvo?.nome.orEmpty()}\" será removido. Esta ação não pode ser desfeita.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        apagando = false
                        val atual = alvo ?: return@TextButton
                        scope.launch {
                            repo.remover(atual)
                            // Se era o último bloco, recria um atual; senão cai no mais recente.
                            repo.garantirBlocoAtual()
                        }
                    },
                ) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { apagando = false }) { Text("Cancelar") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nota", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = { editando = !editando }) {
                        Icon(
                            if (editando) Icons.Filled.Visibility else Icons.Filled.Edit,
                            contentDescription = if (editando) "Visualizar (links clicáveis)" else "Editar",
                        )
                    }
                    IconButton(onClick = onOpenConfig) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do bloco") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            bloco?.let {
                Text(
                    "Criado ${FORMATO_DATA.format(Date(it.dataCriacao))} · " +
                        "Atualizado ${FORMATO_DATA.format(Date(it.dataAtualizacao))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (editando) {
                OutlinedTextField(
                    value = conteudo,
                    onValueChange = { conteudo = it },
                    label = { Text("Escreva sua nota…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                // Modo visualização: URLs viram links clicáveis (ação na engrenagem).
                val anotado = remember(conteudo, corLink) {
                    linkificar(conteudo, corLink) { url -> acaoLink(url) }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (conteudo.isBlank()) {
                        Text(
                            "(bloco vazio)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(anotado, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            // Canto inferior: copiar o conteúdo do bloco atual e apagar o bloco.
            Row(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(conteudo))
                        Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                    },
                    enabled = conteudo.isNotBlank(),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar bloco")
                }
                IconButton(
                    onClick = { removendoDup = true },
                    enabled = conteudo.isNotBlank(),
                ) {
                    Icon(
                        Icons.Filled.PlaylistRemove,
                        contentDescription = "Remover linhas duplicadas",
                    )
                }
                IconButton(
                    onClick = { apagando = true },
                    enabled = bloco != null,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Apagar bloco")
                }
            }
        }
    }
}

/**
 * Remove linhas duplicadas mantendo a **primeira** ocorrência (ordem preservada). A
 * comparação ignora espaços nas pontas; linhas **em branco nunca** são removidas (as
 * capturas usam linha vazia como separador entre trechos). Devolve o texto resultante
 * e quantas linhas foram removidas.
 */
private fun removerLinhasDuplicadas(texto: String): Pair<String, Int> {
    val vistas = HashSet<String>()
    val saida = ArrayList<String>()
    var removidas = 0
    for (linha in texto.split("\n")) {
        val chave = linha.trim()
        if (chave.isEmpty()) {
            saida.add(linha)
        } else if (vistas.add(chave)) {
            saida.add(linha)
        } else {
            removidas++
        }
    }
    return saida.joinToString("\n") to removidas
}

/** Pequeno helper: roda [bloco] em um LaunchedEffect chaveado no valor observado. */
@Composable
private fun <T> LaunchedEffectSync(valor: T, bloco: (T) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(valor) { bloco(valor) }
}

/**
 * Transforma URLs do texto em links clicáveis (Compose [LinkAnnotation.Clickable]); o
 * clique chama [onLink] com a URL, que aplica a ação configurada (copiar/abrir).
 */
private fun linkificar(
    texto: String,
    corLink: Color,
    onLink: (String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    val matcher = Patterns.WEB_URL.matcher(texto)
    var last = 0
    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        if (start > last) append(texto.substring(last, start))
        val url = texto.substring(start, end)
        withLink(
            LinkAnnotation.Clickable(
                tag = url,
                styles = TextLinkStyles(
                    SpanStyle(color = corLink, textDecoration = TextDecoration.Underline),
                ),
                linkInteractionListener = LinkInteractionListener { onLink(url) },
            ),
        ) {
            append(url)
        }
        last = end
    }
    if (last < texto.length) append(texto.substring(last))
}
