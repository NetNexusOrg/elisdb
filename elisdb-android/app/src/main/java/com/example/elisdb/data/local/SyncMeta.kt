package com.example.elisdb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Generic single-value store used for sync metadata (e.g. the pull watermark). */
@Entity(tableName = "sync_meta")
data class SyncMeta(
  @PrimaryKey val key: String,
  val value: Long,
)