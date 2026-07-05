package br.com.notes.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bloco de notas. Todo bloco nasce já salvo (persistido no Room). O nome é editável;
 * [dataCriacao] e [dataAtualizacao] são epoch millis — a atualização é carimbada
 * sempre que o conteúdo (ou o nome) muda, inclusive nas capturas automáticas.
 */
@Entity(tableName = "bloco")
data class Bloco(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val conteudo: String = "",
    val dataCriacao: Long,
    val dataAtualizacao: Long,
)

/**
 * Regra de auto captura: se o texto copiado contiver [texto], a captura vai para um
 * bloco nomeado exatamente [texto] (criado sob demanda) em vez do bloco atual.
 */
@Entity(tableName = "regra_captura")
data class RegraCaptura(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val texto: String,
)
