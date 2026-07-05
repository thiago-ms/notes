package br.com.notes.feature.settings

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.notes.core.data.repo.BackupPrefs
import br.com.notes.core.data.repo.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Centraliza o backup via SAF: escrita/leitura/remoção do `backup.json` na pasta
 * escolhida e o agendamento do backup automático diário (WorkManager). Idêntico ao
 * WatchUp. Sem OAuth, sem Drive API, sem rede direta do app — a pasta pode ser do
 * Google Drive e quem sincroniza é o provider do SAF.
 */
object BackupManager {

    const val NOME_TRABALHO = "notes_backup_diario"
    private const val ARQUIVO = "backup.json"

    /** Executa um backup completo (export + escrita) e registra o resultado. */
    suspend fun executarBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        val agora = System.currentTimeMillis()
        val uriStr = BackupPrefs.getPastaUri(context)
        if (uriStr == null) {
            BackupPrefs.registrarTentativa(context, agora, ok = false)
            return@withContext false
        }
        val ok = runCatching {
            val json = NotesRepository.get(context).exportarJson()
            escrever(context, Uri.parse(uriStr), json)
        }.getOrDefault(false)
        BackupPrefs.registrarTentativa(context, agora, ok)
        ok
    }

    /** Lê o conteúdo do backup salvo na pasta configurada (ou null). */
    suspend fun lerBackup(context: Context): String? = withContext(Dispatchers.IO) {
        val uriStr = BackupPrefs.getPastaUri(context) ?: return@withContext null
        runCatching {
            val arquivo = arquivoBackup(context, Uri.parse(uriStr), criar = false) ?: return@runCatching null
            context.contentResolver.openInputStream(arquivo.uri)?.use { String(it.readBytes()) }
        }.getOrNull()
    }

    /** Remove o backup da pasta configurada. */
    suspend fun apagarBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        val uriStr = BackupPrefs.getPastaUri(context) ?: return@withContext false
        runCatching {
            arquivoBackup(context, Uri.parse(uriStr), criar = false)?.delete() ?: false
        }.getOrDefault(false)
    }

    /** Agenda (ou atualiza) o backup automático diário — só roda com rede. */
    fun agendarDiario(context: Context) {
        val req = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOME_TRABALHO,
            ExistingPeriodicWorkPolicy.UPDATE,
            req,
        )
    }

    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NOME_TRABALHO)
    }

    private fun escrever(context: Context, treeUri: Uri, json: String): Boolean {
        val arquivo = arquivoBackup(context, treeUri, criar = true) ?: return false
        context.contentResolver.openOutputStream(arquivo.uri, "wt")?.use {
            it.write(json.toByteArray())
        } ?: return false
        return true
    }

    private fun arquivoBackup(context: Context, treeUri: Uri, criar: Boolean): DocumentFile? {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val existente = tree.findFile(ARQUIVO)
        return existente ?: if (criar) tree.createFile("application/json", ARQUIVO) else null
    }
}
