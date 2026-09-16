package com.example.elisdb.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists the content hash of each photo at the last successful push, so the
 * sync layer can send references instead of re-uploading unchanged photos.
 */
interface PhotoPushState {
  /** Content hashes of the client's photos at the last successful push. */
  fun getHashes(clienteId: String): Map<String, String>

  /** Records the content hashes of the client's photos after a successful push. */
  fun setHashes(clienteId: String, hashes: Map<String, String>)

  /** Drops any persisted hashes for a client (e.g. when it is permanently deleted). */
  fun clear(clienteId: String)
}

/** SharedPreferences-backed [PhotoPushState]. */
class PrefsPhotoPushState(context: Context) : PhotoPushState {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun getHashes(clienteId: String): Map<String, String> {
    val raw = prefs.getString(key(clienteId), null) ?: return emptyMap()
    return raw
      .split("|")
      .filter { it.isNotBlank() }
      .mapNotNull { entry ->
        val idx = entry.indexOf('=')
        if (idx <= 0) null else entry.substring(0, idx) to entry.substring(idx + 1)
      }
      .toMap()
  }

  override fun setHashes(clienteId: String, hashes: Map<String, String>) {
    prefs.edit { putString(key(clienteId), hashes.entries.joinToString("|") { (id, hash) -> "$id=$hash" }) }
  }

  override fun clear(clienteId: String) {
    prefs.edit { remove(key(clienteId)) }
  }

  private fun key(clienteId: String) = "push_hashes_$clienteId"

  private companion object {
    const val PREFS_NAME = "elisdb_sync_meta"
  }
}
