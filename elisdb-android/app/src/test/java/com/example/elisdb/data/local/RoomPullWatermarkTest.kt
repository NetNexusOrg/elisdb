package com.example.elisdb.data.local

import com.example.elisdb.sync.PULL_WATERMARK_KEY
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomPullWatermarkTest {

  private class FakeSyncMetaDao : SyncMetaDao {
    val store = mutableMapOf<String, Long>()
    override suspend fun get(key: String): Long? = store[key]
    override suspend fun put(meta: SyncMeta) {
      store[meta.key] = meta.value
    }
  }

  @Test
  fun `round-trips the watermark under the shared key`() = runBlocking {
    val dao = FakeSyncMetaDao()
    val watermark = RoomPullWatermark(dao)

    assertNull(watermark.get())
    watermark.set(1234L)
    assertEquals(1234L, watermark.get())
    assertEquals("stored under the shared key", 1234L, dao.store[PULL_WATERMARK_KEY])
  }

  @Test
  fun `set overwrites the previous watermark`() = runBlocking {
    val dao = FakeSyncMetaDao()
    val watermark = RoomPullWatermark(dao)
    watermark.set(10L)
    watermark.set(20L)
    assertEquals(20L, watermark.get())
    assertEquals(1, dao.store.size)
  }
}
