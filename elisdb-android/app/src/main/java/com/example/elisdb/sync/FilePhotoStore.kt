package com.example.elisdb.sync

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Stores photos as JPEG files in app-private storage and reads/writes raw bytes. */
class FilePhotoStore(context: Context) : PhotoStore {
  private val dir = File(context.filesDir, "photos")

  override suspend fun readBytes(uriLocal: String): ByteArray? = withContext(Dispatchers.IO) {
    val file = File(uriLocal)
    if (!file.exists()) return@withContext null
    runCatching { file.readBytes() }.getOrNull()
  }

  override suspend fun storeBytes(bytes: ByteArray, mime: String): String =
    withContext(Dispatchers.IO) {
      dir.mkdirs()
      val extension = extensionFor(mime)
      val dest = File(dir, "foto_${UUID.randomUUID()}.$extension")
      storeAtomically(dest, bytes)
      dest.absolutePath
    }

  override suspend fun contentHashMatches(uriLocal: String, hash: String): Boolean =
    withContext(Dispatchers.IO) {
      val file = File(uriLocal)
      if (!file.exists()) return@withContext false
      val bytes = runCatching { file.readBytes() }.getOrNull()
        ?: return@withContext false
      PhotoHashes.sha256(bytes) == hash
    }

  override suspend fun delete(uriLocal: String) {
    withContext(Dispatchers.IO) { File(uriLocal).delete() }
  }

  override suspend fun listFiles(): List<String> = withContext(Dispatchers.IO) {
    dir.listFiles()?.map { it.absolutePath } ?: emptyList()
  }

  /**
   * Writes [bytes] to [dest] via a temp file in the same directory, fsync, then
   * rename to the final name; falls back to copy+delete where rename is not
   * supported by the filesystem.
   */
  private fun storeAtomically(dest: File, bytes: ByteArray) {
    val tmp = File(dest.parentFile, dest.name + ".tmp")
    FileOutputStream(tmp).use { out ->
      out.write(bytes)
      out.fd.sync()
    }
    if (tmp.renameTo(dest)) return
    FileOutputStream(dest).use { it.write(bytes) }
    tmp.delete()
  }

  companion object {
    /** Maps a MIME type to the file extension used when writing a photo. */
    fun extensionFor(mime: String): String =
      when (mime) {
        "image/webp" -> "webp"
        "image/png" -> "png"
        else -> "jpg"
      }
  }
}