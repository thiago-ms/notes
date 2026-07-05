package br.com.notes.feature.settings

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Backup automático diário em segundo plano. Delega ao [BackupManager]; em caso de
 * falha (ex.: pasta indisponível no momento), pede retry com backoff.
 */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ok = BackupManager.executarBackup(applicationContext)
        return if (ok) Result.success() else Result.retry()
    }
}
