package com.example.elisdb.sync

import com.example.elisdb.data.local.FotoDao
import kotlinx.coroutines.sync.Mutex

/**
 * Shared lock serializing photo-dir materialization (pull) with orphan sweeps,
 * so a sweep never races a concurrent write that is about to insert a file.
 */
object PhotoStoreGuard {
  val mutex = Mutex()
}

/**
 * Reclaims files in the photo store that no photo row references (leftovers from
 * interrupted syncs or failed deletes). Run after a successful pull or on app
 * start, guarded by [PhotoStoreGuard].
 */
class PhotoSweeper(
  private val fotoDao: FotoDao,
  private val photoStore: PhotoStore,
) {
  /**
   * Deletes orphans in bounded batches, refreshing the referenced set between
   * batches so a row inserted during the sweep is not mistaken for stale data.
   * Stops when no orphans remain or [maxFiles] files have been attempted.
   * Returns how many files were removed.
   */
  suspend fun sweep(maxFiles: Int = MAX_ORPHANS_PER_SWEEP): Int {
    var removed = 0
    val attempted = mutableSetOf<String>()
    while (removed < maxFiles) {
      val referenced = fotoDao.getAllUris().toHashSet()
      val batch =
        photoStore
          .listFiles()
          .filter { it !in referenced && it !in attempted }
          .take(minOf(BATCH_SIZE, maxFiles - removed))
      if (batch.isEmpty()) break
      batch.forEach {
        attempted += it
        photoStore.delete(it)
      }
      removed += batch.size
    }
    return removed
  }

  companion object {
    /** Safety cap on the total number of orphans deleted by one sweep. */
    const val MAX_ORPHANS_PER_SWEEP = 500

    /** Files handled per batch, keeping each deletion pass small. */
    const val BATCH_SIZE = 50
  }
}