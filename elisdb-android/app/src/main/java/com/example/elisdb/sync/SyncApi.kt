package com.example.elisdb.sync

import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.remote.ClienteComFotosDto
import com.example.elisdb.remote.PullResponseDto
import com.example.elisdb.remote.PushResultDto
import com.example.elisdb.remote.RecordDto

/** Network transport used by the sync layer. Implemented by [ApiClient]. */
interface SyncApi {
  suspend fun push(
    records: List<RecordDto>,
    timestamp: Long = System.currentTimeMillis() / 1000,
  ): List<PushResultDto>

  suspend fun pull(
    timestamp: Long = System.currentTimeMillis() / 1000,
    since: Long? = null,
    sinceId: String? = null,
  ): PullResponseDto

  /** Deletes on the server; returns true when confirmed gone (204/404), false otherwise. */
  suspend fun delete(id: String, timestamp: Long = System.currentTimeMillis() / 1000, deletedAt: Long? = null): Boolean

  /** Fetches the server's current Unix time (seconds); unauthenticated. */
  suspend fun fetchServerTime(): Long

  /** Fetches the server's latest data timestamp (max updated_at/deleted_at); authenticated. */
  suspend fun fetchLatestUpdatedAt(timestamp: Long = System.currentTimeMillis() / 1000): Long

  /** Fetches a client's edit history (who created/edited it and when). */
  suspend fun fetchClientHistory(id: String, timestamp: Long = System.currentTimeMillis() / 1000): List<AuditEntryDto>

  /** Downloads a photo's raw bytes by id. */
  suspend fun downloadPhoto(id: String, timestamp: Long = System.currentTimeMillis() / 1000): ByteArray

  /** Uploads a photo's raw bytes by id. */
  suspend fun uploadPhoto(id: String, bytes: ByteArray, timestamp: Long = System.currentTimeMillis() / 1000)
}