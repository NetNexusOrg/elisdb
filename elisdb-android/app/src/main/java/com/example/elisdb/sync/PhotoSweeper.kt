package com.example.elisdb.sync

import com.example.elisdb.data.local.DocumentoDao
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
  private val documentoDao: DocumentoDao? = null,
) {
  /** Deletes at most [maxFiles] orphans; returns how many were removed. */
  suspend fun sweep(maxFiles: Int = MAX_ORPHANS_PER_SWEEP): Int {
    // Photos and device-map documents share the directory, so a file is only an
    // orphan when neither table references it.
    val referenced = fotoDao.getAllUris().toHashSet()
    documentoDao?.getAllUris()?.let { referenced += it }
    val orphans = photoStore.listFiles().filter { it !in referenced }.take(maxFiles)
    orphans.forEach { photoStore.delete(it) }
    return orphans.size
  }

  companion object {
    const val MAX_ORPHANS_PER_SWEEP = 50
  }
}