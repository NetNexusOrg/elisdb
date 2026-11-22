package com.example.elisdb.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** The file of a stored photo, used to resolve history thumbnails. */
data class FotoUri(val id: String, val uriLocal: String)

@Dao
interface FotoDao {
  @Query("SELECT * FROM fotos WHERE clienteId = :clienteId AND deletePending = 0 ORDER BY ordem, id")
  fun observeByCliente(clienteId: String): Flow<List<Foto>>

  @Query("SELECT * FROM fotos WHERE clienteId = :clienteId ORDER BY ordem, id")
  suspend fun getByCliente(clienteId: String): List<Foto>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(foto: Foto)

  @Query("DELETE FROM fotos WHERE id IN (:ids)")
  suspend fun deleteByIds(ids: List<String>)

  @Update
  suspend fun update(foto: Foto)

  /** Files of the photos whose ids are listed, used for one history page. */
  @Query("SELECT id, uriLocal FROM fotos WHERE id IN (:ids)")
  suspend fun getUrisByIds(ids: List<String>): List<FotoUri>

  /** Absolute paths of every stored photo, used by the orphan-file sweep. */
  @Query("SELECT uriLocal FROM fotos")
  suspend fun getAllUris(): List<String>
}