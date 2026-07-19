package br.com.notes.core.data.repo

import android.content.Context
import androidx.room.Room
import br.com.notes.core.data.db.NotesDao
import br.com.notes.core.data.db.NotesDatabase
import br.com.notes.core.data.domain.BackupSerializer
import br.com.notes.core.data.model.Bloco
import br.com.notes.core.data.model.RegraCaptura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Contrato de dados do app. Persistência local via Room. Mantém também, **em
 * memória**, qual é o "bloco atual" (o que a aba Nota mostra e o destino padrão das
 * capturas) — não precisa persistir; ao reabrir, cai no bloco mais recente.
 */
interface NotesRepository {

    /** Id do bloco atual (aba Nota / destino padrão da captura). Em memória. */
    val blocoAtualId: StateFlow<Long?>

    fun observarBlocos(): Flow<List<Bloco>>
    fun observarBloco(id: Long): Flow<Bloco?>
    fun observarRegras(): Flow<List<RegraCaptura>>

    /** Cria um bloco novo (já salvo) e o define como atual; devolve o id. */
    suspend fun criarBloco(nome: String? = null): Long

    /** Garante que exista um bloco atual (usa o mais recente ou cria um); devolve o id. */
    suspend fun garantirBlocoAtual(): Long

    fun definirBlocoAtual(id: Long)

    suspend fun renomear(id: Long, nome: String)
    suspend fun atualizarConteudo(id: Long, conteudo: String)
    suspend fun remover(bloco: Bloco)

    /**
     * Mescla todos os blocos com o nome [nome] (ordenados por data de criação) num
     * **bloco novo** chamado "<nome> (mesclado)", sem apagar os originais. Cada trecho
     * recebe uma linha de cabeçalho identificando a origem (nome + data de criação, já
     * que os nomes são iguais). Define o novo bloco como atual e devolve seu id; devolve
     * `null` se houver menos de dois blocos com esse nome (nada a mesclar).
     */
    suspend fun mesclarPorNome(nome: String): Long?

    suspend fun adicionarRegra(texto: String)
    suspend fun removerRegra(regra: RegraCaptura)

    /**
     * Roteia um texto recém-copiado: se casar com uma regra, vai para um bloco com o
     * nome da regra (criado sob demanda); senão, para o bloco atual. Devolve o nome
     * do bloco de destino (para o toast), ou `null` se nada foi capturado.
     *
     * [forcar] = true ignora o anti-duplicação (usado no modo botão flutuante, onde o
     * toque é uma ação explícita do usuário).
     */
    suspend fun capturar(textoCopiado: String, forcar: Boolean = false): String?

    suspend fun exportarJson(): String
    suspend fun importarJson(json: String)

    companion object {
        @Volatile
        private var instance: NotesRepository? = null

        fun get(context: Context): NotesRepository =
            instance ?: synchronized(this) {
                instance ?: run {
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        NotesDatabase::class.java,
                        "notes.db",
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    RoomNotesRepository(db.notesDao()).also { instance = it }
                }
            }
    }
}

class RoomNotesRepository(
    private val dao: NotesDao,
) : NotesRepository {

    private val _blocoAtualId = MutableStateFlow<Long?>(null)
    override val blocoAtualId: StateFlow<Long?> = _blocoAtualId

    // Evita capturar duas vezes o mesmo texto (o clipboard pode notificar repetido).
    private var ultimoCapturado: String? = null

    private fun agora() = System.currentTimeMillis()

    override fun observarBlocos(): Flow<List<Bloco>> = dao.observarBlocos()

    override fun observarBloco(id: Long): Flow<Bloco?> = dao.observarBloco(id)

    override fun observarRegras(): Flow<List<RegraCaptura>> = dao.observarRegras()

    override suspend fun criarBloco(nome: String?): Long {
        val agora = agora()
        val id = dao.inserirBloco(
            Bloco(
                nome = nome?.takeIf { it.isNotBlank() } ?: "Nova nota",
                dataCriacao = agora,
                dataAtualizacao = agora,
            ),
        )
        _blocoAtualId.value = id
        return id
    }

    override suspend fun garantirBlocoAtual(): Long {
        _blocoAtualId.value?.let { atual ->
            if (dao.buscarBloco(atual) != null) return atual
        }
        val recente = dao.blocoMaisRecente()
        val id = recente?.id ?: criarBloco()
        _blocoAtualId.value = id
        return id
    }

    override fun definirBlocoAtual(id: Long) {
        _blocoAtualId.value = id
    }

    override suspend fun renomear(id: Long, nome: String) {
        val bloco = dao.buscarBloco(id) ?: return
        dao.atualizarBloco(bloco.copy(nome = nome, dataAtualizacao = agora()))
    }

    override suspend fun atualizarConteudo(id: Long, conteudo: String) {
        val bloco = dao.buscarBloco(id) ?: return
        if (bloco.conteudo == conteudo) return
        dao.atualizarBloco(bloco.copy(conteudo = conteudo, dataAtualizacao = agora()))
    }

    override suspend fun remover(bloco: Bloco) {
        dao.removerBloco(bloco)
        if (_blocoAtualId.value == bloco.id) {
            _blocoAtualId.value = dao.blocoMaisRecente()?.id
        }
    }

    override suspend fun mesclarPorNome(nome: String): Long? {
        val blocos = dao.buscarTodosPorNome(nome)
        if (blocos.size < 2) return null
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        val partes = blocos.map { b ->
            val cabecalho = "--- de: ${b.nome} (criado ${fmt.format(Date(b.dataCriacao))}) ---"
            if (b.conteudo.isBlank()) cabecalho else "$cabecalho\n${b.conteudo}"
        }
        val agora = agora()
        val id = dao.inserirBloco(
            Bloco(
                nome = "$nome (mesclado)",
                conteudo = partes.joinToString("\n\n"),
                dataCriacao = agora,
                dataAtualizacao = agora,
            ),
        )
        _blocoAtualId.value = id
        return id
    }

    override suspend fun adicionarRegra(texto: String) {
        val t = texto.trim()
        if (t.isEmpty()) return
        val jaExiste = dao.listarRegras().any { it.texto.equals(t, ignoreCase = true) }
        if (!jaExiste) dao.inserirRegra(RegraCaptura(texto = t))
    }

    override suspend fun removerRegra(regra: RegraCaptura) = dao.removerRegra(regra)

    override suspend fun capturar(textoCopiado: String, forcar: Boolean): String? {
        val t = textoCopiado.trim()
        if (t.isEmpty()) return null
        if (!forcar && t == ultimoCapturado) return null

        val regra = dao.listarRegras().firstOrNull { t.contains(it.texto, ignoreCase = true) }
        val alvo: Bloco = if (regra != null) {
            acharOuCriarPorNome(regra.texto)
        } else {
            dao.buscarBloco(garantirBlocoAtual())!!
        }

        // Pula uma linha em branco entre as capturas.
        val novoConteudo = if (alvo.conteudo.isBlank()) t else alvo.conteudo + "\n\n" + t
        dao.atualizarBloco(alvo.copy(conteudo = novoConteudo, dataAtualizacao = agora()))
        ultimoCapturado = t
        return alvo.nome
    }

    private suspend fun acharOuCriarPorNome(nome: String): Bloco {
        dao.buscarPorNome(nome)?.let { return it }
        val agora = agora()
        val id = dao.inserirBloco(Bloco(nome = nome, dataCriacao = agora, dataAtualizacao = agora))
        return dao.buscarBloco(id)!!
    }

    override suspend fun exportarJson(): String =
        BackupSerializer.toJson(dao.listarBlocos(), dao.listarRegras())

    override suspend fun importarJson(json: String) {
        val (blocos, regras) = BackupSerializer.fromJson(json)
        dao.limparBlocos()
        dao.limparRegras()
        blocos.forEach { dao.inserirBloco(it) }
        regras.forEach { dao.inserirRegra(it) }
        _blocoAtualId.value = null
    }
}
