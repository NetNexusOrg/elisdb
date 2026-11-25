package com.example.elisdb.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogoDao {
  @Query("SELECT * FROM catalogo_itens ORDER BY categoria, nome COLLATE NOCASE, id")
  fun observeAll(): Flow<List<CatalogoItem>>

  @Query("SELECT * FROM catalogo_itens ORDER BY categoria, nome COLLATE NOCASE, id")
  suspend fun getAll(): List<CatalogoItem>

  @Query("SELECT * FROM catalogo_itens WHERE id = :id")
  suspend fun getById(id: String): CatalogoItem?

  @Query("SELECT * FROM catalogo_itens WHERE pending = 1")
  suspend fun getPending(): List<CatalogoItem>

  @Query("SELECT * FROM catalogo_itens WHERE categoria = :categoria AND id != :exceptId")
  suspend fun getByCategoria(categoria: String, exceptId: String = ""): List<CatalogoItem>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: CatalogoItem)

  @Upsert
  suspend fun upsert(item: CatalogoItem)

  @Query("UPDATE catalogo_itens SET pending = 0, syncError = NULL WHERE id = :id AND revision = :revision")
  suspend fun markSynced(id: String, revision: Int)

  @Query("UPDATE catalogo_itens SET syncError = :error WHERE id = :id")
  suspend fun setSyncError(id: String, error: String?)

  @Query("DELETE FROM catalogo_itens")
  suspend fun deleteAll()

  @Query("DELETE FROM catalogo_itens WHERE pending = 0")
  suspend fun deleteAllSynced()

  @Query("DELETE FROM catalogo_itens WHERE pending = 0 AND id NOT IN (:ids)")
  suspend fun deleteSyncedExcept(ids: List<String>)
}

@Dao
interface ClienteServicoDao {
  @Transaction
  @Query("SELECT * FROM cliente_servicos WHERE clienteId = :clienteId ORDER BY itemId")
  fun observeForCliente(clienteId: String): Flow<List<ClienteServicoComItem>>

  @Query("SELECT * FROM cliente_servicos WHERE clienteId = :clienteId ORDER BY itemId")
  suspend fun getForCliente(clienteId: String): List<ClienteServico>

  @Query("SELECT * FROM cliente_servicos")
  suspend fun getAll(): List<ClienteServico>

  @Upsert
  suspend fun upsertAll(servicos: List<ClienteServico>)

  @Query("DELETE FROM cliente_servicos WHERE clienteId = :clienteId")
  suspend fun deleteForCliente(clienteId: String)

  @Query("DELETE FROM cliente_servicos")
  suspend fun deleteAll()

  @Query(
    "DELETE FROM cliente_servicos WHERE clienteId NOT IN " +
      "(SELECT id FROM clientes WHERE pending = 1 OR deletePending = 1)"
  )
  suspend fun deleteAllForSyncedClients()

  @Transaction
  suspend fun replaceForCliente(clienteId: String, servicos: List<ClienteServico>) {
    deleteForCliente(clienteId)
    upsertAll(servicos.map { it.copy(clienteId = clienteId) })
  }
}
