package com.example.elisdb.sync

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Creates a recoverable local snapshot before an explicit destructive
 * reconciliation: a consistent copy of the Room database (via `VACUUM INTO`)
 * plus the app's photo directory. The snapshot is written to app-private
 * storage and only the most recent few are kept.
 */
class LocalSnapshotStore(
  context: Context,
  private val maxSnapshots: Int = 3,
) {
  private val appContext = context.applicationContext
  private val snapshotsDir = File(appContext.filesDir, "snapshots")

  /** Creates and returns the snapshot directory; throws if it cannot be completed. */
  suspend fun create(): File = withContext(Dispatchers.IO) {
    snapshotsDir.mkdirs()
    val target = File(snapshotsDir, System.currentTimeMillis().toString())
    if (!target.mkdirs()) throw IllegalStateException("cannot create snapshot directory")
    try {
      val dbSource = appContext.getDatabasePath("elisdb.db")
      if (!dbSource.exists()) throw IllegalStateException("database file not found")
      val dbTarget = File(target, "elisdb.db")
      try {
        val db = SQLiteDatabase.openDatabase(
          dbSource.absolutePath,
          null,
          SQLiteDatabase.OPEN_READONLY,
        )
        try {
          // The path is app-controlled, but escape quotes anyway so the SQL is
          // always well formed. VACUUM INTO is not available on every SQLite
          // bundled with older Android releases, so fall back to copying the
          // database and its WAL sidecars.
          val escaped = dbTarget.absolutePath.replace("'", "''")
          db.execSQL("VACUUM INTO '$escaped'")
        } finally {
          db.close()
        }
      } catch (_: Exception) {
        copyDatabaseWithWal(dbSource, dbTarget)
      }

      val photos = File(appContext.filesDir, "photos")
      if (photos.exists()) {
        photos.copyRecursively(File(target, "photos"), overwrite = true)
      }
      prune()
      target
    } catch (e: Exception) {
      target.deleteRecursively()
      throw e
    }
  }

  /**
   * Fallback for Android SQLite builds without `VACUUM INTO`: copy the main
   * database and its WAL sidecars so Room can recover a consistent snapshot.
   */
  private fun copyDatabaseWithWal(source: File, target: File) {
    source.copyTo(target, overwrite = true)
    listOf("-wal", "-shm").forEach { suffix ->
      val sidecar = File(source.absolutePath + suffix)
      if (sidecar.exists()) sidecar.copyTo(File(target.absolutePath + suffix), overwrite = true)
    }
  }

  private fun prune() {
    val dirs = snapshotsDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: return
    dirs.dropLast(maxSnapshots).forEach { it.deleteRecursively() }
  }
}
