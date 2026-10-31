package com.example.elisdb.sync

import com.example.elisdb.data.local.Documento
import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.DocumentoOutDto
import com.example.elisdb.remote.FotoOutDto

/** Outcome of attempting to materialize one server photo. */
sealed class MaterializeResult {
  /** The existing file/row already matches; reuse it without touching disk. */
  data class Keep(val uriLocal: String) : MaterializeResult()

  /** A new file was written; the DB row must be (re)inserted pointing at it. */
  data class Repointed(val uriLocal: String) : MaterializeResult()
}

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
    return MaterializeResult.Repointed(photoStore.storeBytes(bytes, server.mime))
  }

  /** Same contract as [materialize], for a device-map document. */
  suspend fun materialize(
    existing: Documento?,
    server: DocumentoOutDto,
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
    return MaterializeResult.Repointed(photoStore.storeBytes(bytes, server.mime))
  }
}
