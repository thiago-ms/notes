package br.com.notes.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.notes.core.data.model.Bloco
import br.com.notes.core.data.model.RegraCaptura
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    // --- Blocos ---
    @Query("SELECT * FROM bloco ORDER BY dataAtualizacao DESC")
    fun observarBlocos(): Flow<List<Bloco>>

    @Query("SELECT * FROM bloco WHERE id = :id")
    fun observarBloco(id: Long): Flow<Bloco?>

    @Query("SELECT * FROM bloco WHERE id = :id")
    suspend fun buscarBloco(id: Long): Bloco?

    @Query("SELECT * FROM bloco WHERE nome = :nome LIMIT 1")
    suspend fun buscarPorNome(nome: String): Bloco?

    @Query("SELECT * FROM bloco WHERE nome = :nome ORDER BY dataCriacao ASC")
    suspend fun buscarTodosPorNome(nome: String): List<Bloco>

    @Query("SELECT * FROM bloco ORDER BY dataAtualizacao DESC LIMIT 1")
    suspend fun blocoMaisRecente(): Bloco?

    @Query("SELECT * FROM bloco")
    suspend fun listarBlocos(): List<Bloco>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirBloco(bloco: Bloco): Long

    @Update
    suspend fun atualizarBloco(bloco: Bloco)

    @Delete
    suspend fun removerBloco(bloco: Bloco)

    @Query("DELETE FROM bloco")
    suspend fun limparBlocos()

    // --- Regras de captura ---
    @Query("SELECT * FROM regra_captura ORDER BY texto COLLATE NOCASE")
    fun observarRegras(): Flow<List<RegraCaptura>>

    @Query("SELECT * FROM regra_captura")
    suspend fun listarRegras(): List<RegraCaptura>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirRegra(regra: RegraCaptura): Long

    @Delete
    suspend fun removerRegra(regra: RegraCaptura)

    @Query("DELETE FROM regra_captura")
    suspend fun limparRegras()
}
