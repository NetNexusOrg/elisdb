package com.example.elisdb.data.local

import com.example.elisdb.sync.PULL_WATERMARK_KEY
import com.example.elisdb.sync.PullWatermark

/** Room-backed [PullWatermark] persisting the value inside the app database. */
class RoomPullWatermark(private val dao: SyncMetaDao) : PullWatermark {
  override suspend fun get(): Long? = dao.get(PULL_WATERMARK_KEY)

  override suspend fun set(value: Long) {
    dao.put(SyncMeta(PULL_WATERMARK_KEY, value))
  }

  /** Drops every sync_meta row: the cursor is meaningless on another server. */
  override suspend fun clear() {
    dao.clear()
  }
}