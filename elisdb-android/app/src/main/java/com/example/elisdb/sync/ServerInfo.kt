package com.example.elisdb.sync

/**
 * Lightweight server status fetched by the sync screen: the latest data
 * timestamp (for the pending-downloads signal) and how many clients the
 * server currently stores (shown next to the local total).
 */
data class ServerInfo(
  val latestUpdatedAt: Long,
  /** Null when the server predates the count field, so the UI can show a placeholder. */
  val clientCount: Int? = null,
)
