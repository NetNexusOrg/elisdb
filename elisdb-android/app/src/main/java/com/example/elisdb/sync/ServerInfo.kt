package com.example.elisdb.sync

/**
 * Oldest server API version this app relies on. Increment the server's
 * `API_VERSION` whenever a change matters for compatibility; the app warns when
 * the server reports an older value (or none at all).
 */
const val MIN_SERVER_API_VERSION = 2

/**
 * Lightweight server status fetched by the sync screen: the latest data
 * timestamp (for the pending-downloads signal), how many clients the server
 * currently stores (shown next to the local total) and the server API version.
 */
data class ServerInfo(
  val latestUpdatedAt: Long,
  /** Null when the server predates the count field, so the UI can show a placeholder. */
  val clientCount: Int? = null,
  /** Null when the server predates the API version field. */
  val apiVersion: Int? = null,
)
