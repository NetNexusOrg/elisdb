package com.example.elisdb.sync

/**
 * Lightweight server status fetched by the sync screen: the latest data
 * timestamp (for the pending-downloads signal), how many clients the server
 * currently stores (shown next to the local total) and the server API version.
 *
 * The API version is kept for diagnostics; it is not shown to the app user,
 * who cannot act on a server that needs updating.
 */
data class ServerInfo(
  val latestUpdatedAt: Long,
  /** Null when the server predates the count field, so the UI can show a placeholder. */
  val clientCount: Int? = null,
  /** Null when the server predates the API version field. */
  val apiVersion: Int? = null,
)
