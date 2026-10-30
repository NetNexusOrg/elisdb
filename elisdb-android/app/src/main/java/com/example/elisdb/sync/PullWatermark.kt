package com.example.elisdb.sync

/** Tracks the high-water mark of server `updated_at` seen on pulls. */
interface PullWatermark {
  suspend fun get(): Long?

  suspend fun set(value: Long)

  /** Forgets the cursor, e.g. when the app switches to another server. */
  suspend fun clear()
}

/** Default uppercase key used by the watermark store. */
const val PULL_WATERMARK_KEY = "last_pull_watermark"