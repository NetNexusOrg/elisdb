package com.example.elisdb.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Remembers the identity of the server database last used by this app, so a
 * new/recreated/restored server can be detected before any destructive
 * reconciliation.
 */
interface ServerIdentityStore {
  fun get(): String?

  fun set(instanceId: String)

  fun clear()
}

/** SharedPreferences-backed [ServerIdentityStore]. */
class PrefsServerIdentityStore(context: Context) : ServerIdentityStore {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun get(): String? =
    prefs.getString(KEY_INSTANCE_ID, null)?.takeIf { it.isNotBlank() }

  override fun set(instanceId: String) {
    prefs.edit { putString(KEY_INSTANCE_ID, instanceId) }
  }

  override fun clear() {
    prefs.edit { remove(KEY_INSTANCE_ID) }
  }

  private companion object {
    const val PREFS_NAME = "elisdb_sync_meta"
    const val KEY_INSTANCE_ID = "server_instance_id"
  }
}
