package com.example.elisdb.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Persists [SyncMeta] key-value rows, e.g. the last pull watermark. */
@Dao
interface SyncMetaDao {
  @Query("SELECT value FROM sync_meta WHERE `key` = :key")
  suspend fun get(key: String): Long?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun put(meta: SyncMeta)
}