package com.example.elisdb.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Fixed catalog categories, in the order the UI presents them. */
enum class CategoriaCatalogo(val slug: String, val label: String) {
  ROEDORES("roedores", "Roedores"),
  INSETOS_RASTEJANTES("insetos_rastejantes", "Insetos Rastejantes"),
  INSETOS_VOADORES("insetos_voadores", "Insetos Voadores"),
  TRATAMENTOS("tratamentos", "Tratamentos");

  val isDevice: Boolean
    get() = this != TRATAMENTOS

  companion object {
    fun fromSlug(slug: String): CategoriaCatalogo? = entries.firstOrNull { it.slug == slug }
  }
}

/** A model installed at clients; [nome] is stored as typed, normalization is only for duplicates. */
@Entity(tableName = "catalogo_itens", indices = [Index("categoria")])
data class CatalogoItem(
  @PrimaryKey val id: String,
  val categoria: String,
  val nome: String,
  val nomeNormalizado: String,
  val ativo: Boolean = true,
  val pending: Boolean = false,
  val syncError: String? = null,
  val createdAt: Long = 0,
  val updatedAt: Long = 0,
  val revision: Int = 0,
)

/**
 * One catalog item installed at a client.
 *
 * There is deliberately no foreign key to the catalog: a service can arrive
 * before its item (or reference an item this device has not pulled yet). The UI
 * hides such rows instead of showing a broken name.
 */
@Entity(
  tableName = "cliente_servicos",
  primaryKeys = ["clienteId", "itemId"],
  foreignKeys = [
    ForeignKey(
      entity = Cliente::class,
      parentColumns = ["id"],
      childColumns = ["clienteId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
)
data class ClienteServico(
  val clienteId: String,
  val itemId: String,
  val quantidade: Int? = null,
)

/** Room relation used to resolve the item name/category for a client service. */
data class ClienteServicoComItem(
  @androidx.room.Embedded val servico: ClienteServico,
  @androidx.room.Relation(parentColumn = "itemId", entityColumn = "id")
  val item: CatalogoItem?,
)
