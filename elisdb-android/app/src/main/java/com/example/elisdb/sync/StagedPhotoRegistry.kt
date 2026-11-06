package com.example.elisdb.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Remembers photo files staged by the form but not yet referenced by a photo
 * row, so the orphan sweeper does not delete them while the user is editing.
 */
interface StagedPhotoRegistry {
  fun add(path: String)

  fun remove(path: String)

  fun paths(): Set<String>

  /** Drops entries older than [maxAgeMillis], so abandoned staged files can be swept. */
  fun pruneOlderThan(maxAgeMillis: Long)

  fun clear()
}

/** SharedPreferences-backed [StagedPhotoRegistry]. */
class PrefsStagedPhotoRegistry(context: Context) : StagedPhotoRegistry {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  @Synchronized
  override fun add(path: String) {
    val entries = raw().filterNot { decode(it)?.second == path }.toMutableSet()
    entries += encode(System.currentTimeMillis(), path)
    prefs.edit { putStringSet(KEY_STAGED_PATHS, entries) }
  }

  @Synchronized
  override fun remove(path: String) {
    val entries = raw().filterNot { decode(it)?.second == path }.toSet()
    prefs.edit { putStringSet(KEY_STAGED_PATHS, entries) }
  }

  @Synchronized
  override fun paths(): Set<String> =
    raw().mapNotNull { decode(it)?.second }.toSet()

  @Synchronized
  override fun pruneOlderThan(maxAgeMillis: Long) {
    val cutoff = System.currentTimeMillis() - maxAgeMillis
    val entries = raw().filter { entry ->
      val decoded = decode(entry) ?: return@filter false
      decoded.first >= cutoff
    }.toSet()
    if (entries.size != raw().size) {
      prefs.edit { putStringSet(KEY_STAGED_PATHS, entries) }
    }
  }

  override fun clear() {
    prefs.edit { remove(KEY_STAGED_PATHS) }
  }

  private fun raw(): Set<String> = prefs.getStringSet(KEY_STAGED_PATHS, emptySet())?.toSet().orEmpty()

  private fun encode(timestamp: Long, path: String): String = "$timestamp\t$path"

  private fun decode(entry: String): Pair<Long, String>? {
    val separator = entry.indexOf('\t')
    if (separator <= 0) return null
    val timestamp = entry.substring(0, separator).toLongOrNull() ?: return null
    return timestamp to entry.substring(separator + 1)
  }

  private companion object {
    const val PREFS_NAME = "elisdb_sync_meta"
    const val KEY_STAGED_PATHS = "staged_photo_paths"
  }
}
