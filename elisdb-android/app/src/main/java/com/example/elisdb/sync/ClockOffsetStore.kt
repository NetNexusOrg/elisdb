package com.example.elisdb.sync

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the device↔server clock offset (server_epoch_sec − device_epoch_sec)
 * measured from the server's reported time, so records stamped while offline use
 * the last-known-good timescale for fair last-write-wins.
 */
interface ClockOffsetStore {
  /** Server−device clock offset in seconds, or null when never measured. */
  fun getOffsetSeconds(): Long?

  /** Persists a newly measured offset (seconds). */
  fun setOffsetSeconds(offsetSeconds: Long)
}

/** SharedPreferences-backed [ClockOffsetStore]. */
class PrefsClockOffsetStore(context: Context) : ClockOffsetStore {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun getOffsetSeconds(): Long? =
    if (prefs.contains(KEY_OFFSET)) prefs.getLong(KEY_OFFSET, 0) else null

  override fun setOffsetSeconds(offsetSeconds: Long) {
    prefs.edit().putLong(KEY_OFFSET, offsetSeconds).apply()
  }

  private companion object {
    const val PREFS_NAME = "elisdb_sync_meta"
    const val KEY_OFFSET = "clock_offset_seconds"
  }
}
