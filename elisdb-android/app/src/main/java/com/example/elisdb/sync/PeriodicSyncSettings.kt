package com.example.elisdb.sync

import android.content.Context

/** User preference for the opt-in periodic background sync. */
interface PeriodicSyncSettings {
  fun isEnabled(): Boolean

  fun setEnabled(enabled: Boolean)
}

/** Persists the periodic-sync toggle; disabled by default. */
class PrefsPeriodicSyncSettings(context: Context) : PeriodicSyncSettings {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

  override fun setEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
  }

  private companion object {
    const val PREFS_NAME = "elisdb_periodic_sync"
    const val KEY_ENABLED = "enabled"
  }
}
