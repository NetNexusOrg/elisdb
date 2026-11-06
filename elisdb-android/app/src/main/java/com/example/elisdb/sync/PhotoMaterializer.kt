package com.example.elisdb.sync

import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.FotoOutDto

/** Outcome of attempting to materialize one server photo. */
sealed class MaterializeResult {
  /** The existing file/row already matches; reuse it without touching disk. */
  data class Keep(val uriLocal: String) : MaterializeResult()

  /** A new file was written; the DB row must be (re)inserted pointing at it. */
  data class Repointed(val uriLocal: String) : MaterializeResult()
}

/** Raised when downloaded bytes do not match the server's declared image MIME. */
class PhotoMimeMismatchException(message: String) : IllegalStateException(message)

/**
 * Content-aware photo materialization for pull: decides whether an existing
 * local photo matches a server photo (same hash + mime/ordem) and downloads
 * changed photos into the store's two-phase write.
 */
class PhotoMaterializer(private val photoStore: PhotoStore) {

  /** Writes [server] to a new file unless [existing] already matches it. */
  suspend fun materialize(
    existing: Foto?,
    server: FotoOutDto,
    download: suspend (String) -> ByteArray,
  ): MaterializeResult {
    if (existing != null &&
      existing.mime == server.mime &&
      existing.ordem == server.ordem &&
      server.hash != null &&
      photoStore.contentHashMatches(existing.uriLocal, server.hash)
    ) {
      return MaterializeResult.Keep(existing.uriLocal)
    }
    val bytes = download(server.id)
    val sniffed = sniffImageMime(bytes)
    if (sniffed == null || sniffed != server.mime) {
      throw PhotoMimeMismatchException(
        "downloaded photo ${server.id} does not match declared mime ${server.mime}",
      )
    }
    return MaterializeResult.Repointed(photoStore.storeBytes(bytes, server.mime))
  }

  private fun sniffImageMime(bytes: ByteArray): String? {
    if (bytes.size >= 3 &&
      bytes[0] == 0xFF.toByte() &&
      bytes[1] == 0xD8.toByte() &&
      bytes[2] == 0xFF.toByte()
    ) {
      return "image/jpeg"
    }
    if (bytes.size >= 8 &&
      bytes[0] == 0x89.toByte() &&
      bytes[1] == 0x50.toByte() &&
      bytes[2] == 0x4E.toByte() &&
      bytes[3] == 0x47.toByte() &&
      bytes[4] == 0x0D.toByte() &&
      bytes[5] == 0x0A.toByte() &&
      bytes[6] == 0x1A.toByte() &&
      bytes[7] == 0x0A.toByte()
    ) {
      return "image/png"
    }
    if (bytes.size >= 12 &&
      bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray(Charsets.US_ASCII)) &&
      bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray(Charsets.US_ASCII))
    ) {
      return "image/webp"
    }
    return null
  }
}
