package br.com.notes.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.com.notes.core.data.repo.AutoCaptureState
import br.com.notes.core.data.repo.BackupPrefs
import br.com.notes.core.data.repo.CaptureMode
import br.com.notes.core.data.repo.CapturePrefs
import br.com.notes.core.data.repo.LinkAcao
import br.com.notes.core.data.repo.NotesRepository
import br.com.notes.core.ui.component.PushScreenScaffold
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FORMATO_DATA = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

/**
 * Configurações — auto captura (toggle em memória + regras de destino) e backup via
 * SAF (idêntico ao WatchUp: escolhe uma pasta, que pode ser do Google Drive, e
 * grava/lê o `backup.json`; manual + automático diário via WorkManager).
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NotesRepository.get(context) }
    val scope = rememberCoroutineScope()

    PushScreenScaffold(title = "Configurações", onBack = onBack) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            SecaoAutoCaptura(repo, scope)
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            SecaoLinks()
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            SecaoBackup(repo, scope)
        }
    }
}

@Composable
private fun SecaoAutoCaptura(
    repo: NotesRepository,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val context = LocalContext.current
    val ativo by AutoCaptureState.ativo.collectAsStateWithLifecycle()
    val regras by repo.observarRegras().collectAsStateWithLifecycle(emptyList())
    var novaRegra by remember { mutableStateOf("") }
    var modo by remember { mutableStateOf(CapturePrefs.getModo(context)) }
    var pedirAcessibilidade by remember { mutableStateOf(false) }

    // Ao voltar da tela de permissão de overlay: se concedida, sobe o botão; senão,
    // desliga a auto captura.
    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (FloatingControl.temPermissaoOverlay(context)) {
            FloatingControl.iniciar(context)
        } else {
            AutoCaptureState.definir(false)
            Toast.makeText(context, "Permissão de sobreposição necessária.", Toast.LENGTH_SHORT).show()
        }
    }

    fun aplicarLiga(on: Boolean) {
        AutoCaptureState.definir(on)
        when (modo) {
            CaptureMode.ACESSIBILIDADE -> {
                FloatingControl.parar(context)
                if (on && !AcessibilidadeUtil.servicoAtivo(context)) pedirAcessibilidade = true
            }
            CaptureMode.FLUTUANTE -> {
                if (on) {
                    if (FloatingControl.temPermissaoOverlay(context)) {
                        FloatingControl.iniciar(context)
                    } else {
                        overlayLauncher.launch(FloatingControl.intentPermissaoOverlay(context))
                    }
                } else {
                    FloatingControl.parar(context)
                }
            }
        }
    }

    fun mudarModo(novo: CaptureMode) {
        if (novo == modo) return
        modo = novo
        CapturePrefs.setModo(context, novo)
        if (ativo) aplicarLiga(true) // reaplica no novo modo (para/inicia o que for preciso)
    }

    if (pedirAcessibilidade) {
        AlertDialog(
            onDismissRequest = { pedirAcessibilidade = false },
            title = { Text("Ativar captura em segundo plano") },
            text = {
                Text(
                    "Para capturar o que você copia em outros apps, habilite o serviço " +
                        "de acessibilidade do Notas. Sem isso, a captura só funciona com o " +
                        "app aberto na frente.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pedirAcessibilidade = false
                    AcessibilidadeUtil.abrirAjustes(context)
                }) { Text("Abrir Ajustes") }
            },
            dismissButton = { TextButton(onClick = { pedirAcessibilidade = false }) { Text("Agora não") } },
        )
    }

    Text("Auto captura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text(
        "Enquanto ligada, o texto copiado é adicionado ao bloco atual. Desliga sozinha " +
            "ao fechar o app (o estado não é salvo).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))

    // Seletor de modo.
    ModoOpcao(
        selecionado = modo == CaptureMode.ACESSIBILIDADE,
        titulo = "Segundo plano (acessibilidade)",
        descricao = "Captura tudo que você copia, automaticamente. Requer o serviço de acessibilidade.",
        onClick = { mudarModo(CaptureMode.ACESSIBILIDADE) },
    )
    ModoOpcao(
        selecionado = modo == CaptureMode.FLUTUANTE,
        titulo = "Botão flutuante",
        descricao = "Um botão fica na tela; ao copiar algo ele acende e você toca para colar. " +
            "Requer permissão de sobreposição (e uma notificação fixa).",
        onClick = { mudarModo(CaptureMode.FLUTUANTE) },
    )

    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            if (ativo) "Auto captura ligada" else "Auto captura desligada",
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = ativo, onCheckedChange = { aplicarLiga(it) })
    }

    if (ativo && modo == CaptureMode.ACESSIBILIDADE && !AcessibilidadeUtil.servicoAtivo(context)) {
        Spacer(Modifier.height(6.dp))
        Text(
            "Habilite a acessibilidade do Notas para capturar de outros apps.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = { AcessibilidadeUtil.abrirAjustes(context) }) {
            Text("Abrir Ajustes de Acessibilidade")
        }
    }

    if (ativo && modo == CaptureMode.FLUTUANTE && !FloatingControl.temPermissaoOverlay(context)) {
        Spacer(Modifier.height(6.dp))
        Text(
            "Conceda a permissão de sobreposição para o botão flutuante aparecer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = { overlayLauncher.launch(FloatingControl.intentPermissaoOverlay(context)) }) {
            Text("Conceder permissão")
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("Regras de destino", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text(
        "Se o texto copiado contiver um destes termos, a captura vai para um bloco com " +
            "esse nome (criado se não existir). Ex.: \"instagram.com\".",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = novaRegra,
            onValueChange = { novaRegra = it },
            label = { Text("Novo termo") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            val t = novaRegra.trim()
            if (t.isNotEmpty()) {
                novaRegra = ""
                scope.launch { repo.adicionarRegra(t) }
            }
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Adicionar termo")
        }
    }

    if (regras.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        regras.forEach { regra ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(regra.texto, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { scope.launch { repo.removerRegra(regra) } }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remover termo")
                }
            }
        }
    }
}

@Composable
private fun SecaoBackup(
    repo: NotesRepository,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val context = LocalContext.current

    var pastaUri by remember { mutableStateOf(BackupPrefs.getPastaUri(context)) }
    var ultimoBackup by remember { mutableLongStateOf(BackupPrefs.getUltimoBackup(context)) }
    var ultimaTentativa by remember { mutableLongStateOf(BackupPrefs.getUltimaTentativa(context)) }
    var ultimaTentativaOk by remember { mutableStateOf(BackupPrefs.getUltimaTentativaOk(context)) }
    var autoAtivo by remember { mutableStateOf(BackupPrefs.getAutoAtivo(context)) }
    var salvando by remember { mutableStateOf(false) }
    var confirmarRestauracao by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(BackupManager.NOME_TRABALHO)
        .collectAsStateWithLifecycle(emptyList())
    val estadoAuto = workInfos.firstOrNull()?.state

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun recarregarStatus() {
        ultimoBackup = BackupPrefs.getUltimoBackup(context)
        ultimaTentativa = BackupPrefs.getUltimaTentativa(context)
        ultimaTentativaOk = BackupPrefs.getUltimaTentativaOk(context)
    }

    val seletorPasta = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            BackupPrefs.setPastaUri(context, uri.toString())
            pastaUri = uri.toString()
        }
    }

    if (confirmarRestauracao) {
        AlertDialog(
            onDismissRequest = { confirmarRestauracao = false },
            title = { Text("Restaurar backup?") },
            text = { Text("Isso substitui todos os seus blocos atuais pelo conteúdo do backup.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRestauracao = false
                    scope.launch {
                        val json = BackupManager.lerBackup(context)
                        if (json == null) {
                            toast("Nenhum backup encontrado na pasta.")
                        } else {
                            repo.importarJson(json)
                            toast("Backup restaurado")
                        }
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { confirmarRestauracao = false }) { Text("Cancelar") } },
        )
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Apagar backup?") },
            text = { Text("O arquivo de backup na pasta será removido. Seus blocos no app não são afetados.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarExclusao = false
                    scope.launch {
                        val ok = BackupManager.apagarBackup(context)
                        toast(if (ok) "Backup apagado" else "Nenhum backup para apagar.")
                    }
                }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }

    Text("Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text(
        "Escolha uma pasta (pode ser do Google Drive) para guardar o backup dos seus blocos.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            val nomePasta = pastaUri?.let { nomeDaPasta(context, it) }
            Text("Pasta: ${nomePasta ?: "não configurada"}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Último backup: ${if (ultimoBackup > 0) FORMATO_DATA.format(Date(ultimoBackup)) else "nunca"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val emProgresso = salvando || estadoAuto == WorkInfo.State.RUNNING
            val statusLinha = when {
                emProgresso -> "Status: fazendo backup…"
                ultimaTentativa > 0 && !ultimaTentativaOk && ultimaTentativa >= ultimoBackup ->
                    "Status: última tentativa (${FORMATO_DATA.format(Date(ultimaTentativa))}) falhou"
                autoAtivo && estadoAuto == WorkInfo.State.ENQUEUED -> "Status: automático agendado"
                else -> null
            }
            if (statusLinha != null) {
                Spacer(Modifier.height(4.dp))
                val cor = if (statusLinha.contains("falhou")) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(statusLinha, style = MaterialTheme.typography.bodySmall, color = cor)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { seletorPasta.launch(null) }) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (pastaUri == null) "Escolher pasta" else "Trocar pasta")
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    val temPasta = pastaUri != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Backup automático (diário)", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Uma vez por dia, em segundo plano, quando houver rede.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = autoAtivo,
            enabled = temPasta,
            onCheckedChange = { on ->
                autoAtivo = on
                BackupPrefs.setAutoAtivo(context, on)
                if (on) BackupManager.agendarDiario(context) else BackupManager.cancelar(context)
            },
        )
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = {
            scope.launch {
                salvando = true
                val ok = BackupManager.executarBackup(context)
                salvando = false
                recarregarStatus()
                toast(if (ok) "Backup salvo" else "Não foi possível salvar o backup.")
            }
        },
        enabled = temPasta && !salvando,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (salvando) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text("Fazer backup agora")
        }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { confirmarRestauracao = true },
        enabled = temPasta,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Restaurar backup") }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { confirmarExclusao = true },
        enabled = temPasta,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Apagar backup") }
}

@Composable
private fun SecaoLinks() {
    val context = LocalContext.current
    var acao by remember { mutableStateOf(CapturePrefs.getLinkAcao(context)) }

    fun definir(nova: LinkAcao) {
        acao = nova
        CapturePrefs.setLinkAcao(context, nova)
    }

    Text("Links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text(
        "Ao tocar num link no modo visualização do bloco (ícone de olho na aba Nota):",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    ModoOpcao(
        selecionado = acao == LinkAcao.COPIAR,
        titulo = "Copiar para o clipboard",
        descricao = "Toca no link e ele vai para o clipboard (padrão).",
        onClick = { definir(LinkAcao.COPIAR) },
    )
    ModoOpcao(
        selecionado = acao == LinkAcao.ABRIR,
        titulo = "Abrir no navegador",
        descricao = "Toca no link e abre direto no navegador.",
        onClick = { definir(LinkAcao.ABRIR) },
    )
}

@Composable
private fun ModoOpcao(
    selecionado: Boolean,
    titulo: String,
    descricao: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selecionado, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f).padding(top = 12.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                descricao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun nomeDaPasta(context: android.content.Context, treeUri: String): String? =
    runCatching {
        androidx.documentfile.provider.DocumentFile.fromTreeUri(context, Uri.parse(treeUri))?.name
    }.getOrNull()
