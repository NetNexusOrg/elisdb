package com.example.elisdb.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists when the last successful synchronization finished, so the
 * synchronization screen can show it across app restarts.
 */
interface LastSyncStore {
  /** Epoch seconds of the last successful sync, or null when never completed. */
  fun getLastSuccessAt(): Long?

  /** Persists the epoch seconds of a just-completed successful sync. */
  fun setLastSuccessAt(epochSeconds: Long)
}

/** SharedPreferences-backed [LastSyncStore]. */
class PrefsLastSyncStore(context: Context) : LastSyncStore {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun getLastSuccessAt(): Long? =
    if (prefs.contains(KEY_LAST_SUCCESS)) prefs.getLong(KEY_LAST_SUCCESS, 0) else null

  override fun setLastSuccessAt(epochSeconds: Long) {
    prefs.edit { putLong(KEY_LAST_SUCCESS, epochSeconds) }
  }

  private companion object {
    const val PREFS_NAME = "elisdb_sync_meta"
    const val KEY_LAST_SUCCESS = "last_success_at"
  }
}
