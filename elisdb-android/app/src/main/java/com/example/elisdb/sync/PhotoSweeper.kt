package com.example.elisdb.sync

import com.example.elisdb.data.local.FotoDao
import java.io.File
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
  private val stagedPhotoRegistry: StagedPhotoRegistry? = null,
  private val minAgeSeconds: Long = DEFAULT_MIN_AGE_SECONDS,
) {
  /**
   * Deletes orphans in bounded batches, refreshing the referenced set between
   * batches so a row inserted during the sweep is not mistaken for stale data.
   * Stops when no orphans remain or [maxFiles] files have been attempted.
   * Returns how many files were removed.
   *
   * Files registered as staged (an open form that has not saved yet) and files
   * younger than [minAgeSeconds] are left alone, so a sweep cannot race a photo
   * the user just picked or captured.
   */
  suspend fun sweep(maxFiles: Int = MAX_ORPHANS_PER_SWEEP): Int {
    stagedPhotoRegistry?.pruneOlderThan(DEFAULT_STAGED_RETENTION_MILLIS)
    val cutoff = System.currentTimeMillis() - minAgeSeconds * 1_000
    var removed = 0
    val attempted = mutableSetOf<String>()
    while (removed < maxFiles) {
      val referenced = fotoDao.getAllUris().toHashSet()
      // Refresh staged paths each batch: a photo can be staged while a sweep is
      // already running, and it must not be treated as an orphan.
      val staged = stagedPhotoRegistry?.paths().orEmpty()
      val batch =
        photoStore
          .listFiles()
          .filter {
            it !in referenced &&
              it !in attempted &&
              it !in staged &&
              !isTooYoung(it, cutoff)
          }
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

  private fun isTooYoung(path: String, cutoff: Long): Boolean {
    val file = File(path)
    return file.exists() && file.lastModified() > cutoff
  }

  companion object {
    /** Safety cap on the total number of orphans deleted by one sweep. */
    const val MAX_ORPHANS_PER_SWEEP = 500

    /** Files handled per batch, keeping each deletion pass small. */
    const val BATCH_SIZE = 50

    /** Default grace period for a file that no row references yet. */
    const val DEFAULT_MIN_AGE_SECONDS = 3_600L

    /** Abandoned staged entries are forgotten after this long. */
    const val DEFAULT_STAGED_RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1_000
  }
}