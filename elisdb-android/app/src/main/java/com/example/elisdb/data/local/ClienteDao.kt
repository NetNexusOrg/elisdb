package com.example.elisdb.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
  @Query("SELECT * FROM clientes WHERE deletePending = 0 ORDER BY nome COLLATE NOCASE")
  fun observeAll(): Flow<List<Cliente>>

  @Query("SELECT * FROM clientes ORDER BY nome COLLATE NOCASE")
  fun observeAllIncludingDeleted(): Flow<List<Cliente>>

  @Query("SELECT COUNT(*) FROM clientes WHERE deletePending = 0")
  fun observeCount(): Flow<Int>

  @Query("SELECT * FROM clientes")
  suspend fun getAll(): List<Cliente>

  @Transaction
  @Query("SELECT * FROM clientes WHERE deletePending = 0 ORDER BY nome COLLATE NOCASE")
  fun observeAllComFotos(): Flow<List<ClienteComFotos>>

  @Transaction
  @Query("SELECT * FROM clientes WHERE id = :id AND deletePending = 0")
  fun observeClienteComFotos(id: String): Flow<ClienteComFotos?>

  @Transaction
  @Query("SELECT * FROM clientes WHERE id = :id AND deletePending = 0")
  suspend fun getClienteComFotos(id: String): ClienteComFotos?

  @Query("SELECT * FROM clientes WHERE id = :id")
  suspend fun getById(id: String): Cliente?

  @Query("SELECT * FROM clientes WHERE pending = 1")
  suspend fun getPending(): List<Cliente>

  @Query("SELECT * FROM clientes WHERE deletePending = 1")
  suspend fun getPendingDeletions(): List<Cliente>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(cliente: Cliente)

  /** Inserts or updates a client without replacing the row, so the fotos FK
   *  CASCADE is never triggered when the client already exists locally. */
  @Upsert
  suspend fun upsert(cliente: Cliente)

  @Update
  suspend fun update(cliente: Cliente)

  /**
   * Clears [Cliente.pending] only while the row still holds the revision that was
   * pushed. Returns 1 when the ack applied, 0 when the record was edited while
   * the push was in flight (the new edit must stay pending and be sent again).
   *
   * The comparison uses [Cliente.revision] rather than `updatedAt` because the
   * timestamps have one-second resolution: an edit made in the same second as
   * the pushed snapshot would otherwise be acknowledged and silently lost.
   */
  @Query("UPDATE clientes SET pending = 0 WHERE id = :id AND revision = :seenRevision")
  suspend fun markSynced(id: String, seenRevision: Long): Int

  @Delete
  suspend fun delete(cliente: Cliente)
}