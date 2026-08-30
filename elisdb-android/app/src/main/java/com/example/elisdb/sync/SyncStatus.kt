package com.example.elisdb.sync

/** Observable sync state surfaced to the UI. */
enum class SyncStatus {
  IDLE,
  IN_PROGRESS,
  SUCCESS,
  ERROR,
}

/** Reason for a failed sync, surfaced so the UI can show an actionable message. */
enum class SyncFault {
  /** Missing or invalid server base URL or signing key in the build config. */
  CONFIG,
  /** Server rejected the request (unknown/missing key or invalid signature). */
  AUTH,
  /** The device clock is outside the server's accepted freshness window. */
  CLOCK,
  /** Transient connectivity or server error. */
  NETWORK,
}

/** Sealed result of a sync attempt. */
sealed class SyncOutcome {
  data object Success : SyncOutcome()
  data class SuccessWithStatuses(val accepted: Int, val stale: List<String>) : SyncOutcome()
  data class DuplicateMerged(val nome: String) : SyncOutcome()
  data object AuthFailure : SyncOutcome()
  data object ClockFailure : SyncOutcome()
  data object TransientError : SyncOutcome()
  data object ConfigFault : SyncOutcome()
}

/** Reads/writes photo bytes for push and pull. Implemented by the app store. */
interface PhotoStore {
  /** Returns the raw bytes of the file at [uriLocal], or null if missing. */
  suspend fun readBytes(uriLocal: String): ByteArray?

  /** Writes [bytes] to a new app-private file with an extension derived from [mime]; returns its absolute path. */
  suspend fun storeBytes(bytes: ByteArray, mime: String): String

  /** Permanently removes the file at [uriLocal], if present. */
  suspend fun delete(uriLocal: String)

  /** Returns true when the file at [uriLocal] hashes to [hash] (SHA-256 hex). */
  suspend fun contentHashMatches(uriLocal: String, hash: String): Boolean

  /** Returns the absolute paths of every file currently managed by the store. */
  suspend fun listFiles(): List<String>
}