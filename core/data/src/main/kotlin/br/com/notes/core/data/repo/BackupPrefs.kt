package br.com.notes.core.data.repo

import android.content.Context

/**
 * Guarda a configuração de backup: a URI (SAF) da pasta escolhida pelo usuário e o
 * momento do último backup. Nada de rede — a pasta pode ser do Google Drive, mas
 * quem sincroniza é o próprio provider do SAF. Idêntico ao WatchUp.
 */
object BackupPrefs {
    private const val NOME = "notes_backup"
    private const val K_URI = "pasta_uri"
    private const val K_ULTIMO = "ultimo_backup"
    private const val K_AUTO = "auto_ativo"
    private const val K_TENTATIVA = "ultima_tentativa"
    private const val K_TENTATIVA_OK = "ultima_tentativa_ok"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NOME, Context.MODE_PRIVATE)

    fun getPastaUri(context: Context): String? = prefs(context).getString(K_URI, null)

    fun setPastaUri(context: Context, uri: String?) {
        prefs(context).edit().apply {
            if (uri == null) remove(K_URI) else putString(K_URI, uri)
        }.apply()
    }

    /** Epoch millis do último backup bem-sucedido; 0 = nunca. */
    fun getUltimoBackup(context: Context): Long = prefs(context).getLong(K_ULTIMO, 0L)

    fun setUltimoBackup(context: Context, millis: Long) {
        prefs(context).edit().putLong(K_ULTIMO, millis).apply()
    }

    /** Backup automático diário ligado/desligado. */
    fun getAutoAtivo(context: Context): Boolean = prefs(context).getBoolean(K_AUTO, false)

    fun setAutoAtivo(context: Context, ativo: Boolean) {
        prefs(context).edit().putBoolean(K_AUTO, ativo).apply()
    }

    /** Momento e resultado da última tentativa (manual ou automática). */
    fun getUltimaTentativa(context: Context): Long = prefs(context).getLong(K_TENTATIVA, 0L)

    fun getUltimaTentativaOk(context: Context): Boolean = prefs(context).getBoolean(K_TENTATIVA_OK, true)

    fun registrarTentativa(context: Context, millis: Long, ok: Boolean) {
        prefs(context).edit()
            .putLong(K_TENTATIVA, millis)
            .putBoolean(K_TENTATIVA_OK, ok)
            .apply()
        if (ok) setUltimoBackup(context, millis)
    }
}
