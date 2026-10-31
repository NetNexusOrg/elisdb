package com.example.elisdb.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DocumentoDao {
  @Query("SELECT * FROM documentos WHERE clienteId = :clienteId AND deletePending = 0 ORDER BY ordem, id")
  suspend fun getByCliente(clienteId: String): List<Documento>

  @Query("SELECT * FROM documentos WHERE clienteId = :clienteId ORDER BY ordem, id")
  suspend fun getAllByCliente(clienteId: String): List<Documento>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(documento: Documento)

  @Query("DELETE FROM documentos WHERE id IN (:ids)")
  suspend fun deleteByIds(ids: List<String>)

  @Update
  suspend fun update(documento: Documento)

  /** Absolute paths of every stored document, used by the orphan-file sweep. */
  @Query("SELECT uriLocal FROM documentos")
  suspend fun getAllUris(): List<String>
}
