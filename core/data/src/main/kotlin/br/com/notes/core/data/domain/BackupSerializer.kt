package br.com.notes.core.data.domain

import br.com.notes.core.data.model.Bloco
import br.com.notes.core.data.model.RegraCaptura
import org.json.JSONArray
import org.json.JSONObject

/**
 * (De)serialização do backup em JSON (via `org.json`, sem lib extra). Um único
 * `backup.json` guarda os blocos (com datas de criação/atualização) e as regras de
 * auto captura. Usado pelo backup via SAF (idêntico ao WatchUp).
 */
object BackupSerializer {

    private const val VERSION = 1

    fun toJson(blocos: List<Bloco>, regras: List<RegraCaptura>): String {
        val root = JSONObject()
        root.put("version", VERSION)

        val arrBlocos = JSONArray()
        blocos.forEach {
            arrBlocos.put(
                JSONObject()
                    .put("id", it.id)
                    .put("nome", it.nome)
                    .put("conteudo", it.conteudo)
                    .put("dataCriacao", it.dataCriacao)
                    .put("dataAtualizacao", it.dataAtualizacao),
            )
        }
        root.put("blocos", arrBlocos)

        val arrRegras = JSONArray()
        regras.forEach { arrRegras.put(it.texto) }
        root.put("regras", arrRegras)

        return root.toString(2)
    }

    fun fromJson(json: String): Pair<List<Bloco>, List<RegraCaptura>> {
        val root = JSONObject(json)

        val blocos = root.optJSONArray("blocos")?.let { arr ->
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Bloco(
                    id = o.getLong("id"),
                    nome = o.getString("nome"),
                    conteudo = o.optString("conteudo", ""),
                    dataCriacao = o.optLong("dataCriacao", 0L),
                    dataAtualizacao = o.optLong("dataAtualizacao", 0L),
                )
            }
        }.orEmpty()

        val regras = root.optJSONArray("regras")?.let { arr ->
            (0 until arr.length()).map { RegraCaptura(texto = arr.getString(it)) }
        }.orEmpty()

        return blocos to regras
    }
}
