package com.example.elisdb

import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.CatalogoItem
import com.example.elisdb.data.local.CategoriaCatalogo
import com.example.elisdb.data.local.ClienteServicoComItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Catalog stubs for instrumentation fakes focused on other features. */
abstract class EmptyCatalogoRepository : ClienteRepository {
  override val catalogo: Flow<List<CatalogoItem>> = flowOf(emptyList())

  override fun observeServicos(clienteId: String): Flow<List<ClienteServicoComItem>> =
    flowOf(emptyList())

  override suspend fun getCatalogo(): List<CatalogoItem> = emptyList()

  override suspend fun getServicos(clienteId: String): List<ClienteServicoComItem> =
    emptyList()

  override suspend fun saveCatalogoItem(
    id: String?,
    nome: String,
    categoria: CategoriaCatalogo,
  ): String = id ?: "catalogo-test"

  override suspend fun setCatalogoAtivo(id: String, ativo: Boolean) = Unit
}
