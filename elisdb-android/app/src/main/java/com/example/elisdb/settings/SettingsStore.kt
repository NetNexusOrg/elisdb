package com.example.elisdb.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.elisdb.BuildConfig

/** Effective server connection settings for the app. */
data class ServerSettings(
  val baseUrl: String,
  val signingKey: String,
  /** User name carried by the imported configuration, blank when unknown. */
  val userName: String = "",
)

/** Storage for runtime server connection settings (base URL + signing key). */
interface SettingsStore {
  /** Currently effective values: runtime override when saved, else build-time default. */
  fun settings(): ServerSettings

  /** Persists runtime overrides. Blank values are stored verbatim. */
  fun save(baseUrl: String, signingKey: String, userName: String = "")
}

/**
 * Stores the runtime server connection overrides (base URL + signing key seed)
 * encrypted at rest via [EncryptedSharedPreferences] (master key in the Android
 * Keystore). Values saved by older plain-text versions are migrated on first use
 * and the plain-text file is cleared, so existing configurations keep working.
 *
 * Absent a saved value, [settings] reflects the build-time defaults from
 * `BuildConfig`, so existing builds keep working unchanged.
 */
class PrefsSettingsStore(
  context: Context,
  /** Seam for tests: opens the encrypted store (which may be unavailable). */
  openEncrypted: (Context) -> SharedPreferences = ::createEncryptedStore,
) : SettingsStore {
  private val appContext = context.applicationContext
  private val prefs: SharedPreferences = openSettings(appContext, openEncrypted)

  override fun settings(): ServerSettings =
    ServerSettings(
      baseUrl = prefs.getString(KEY_BASE_URL, null) ?: BuildConfig.SERVER_BASE_URL,
      // The signing key is never taken from a build-time constant: it must come
      // from a per-device import, so a key baked into the APK can never be used
      // to authenticate. When no key has been imported, this is blank and the
      // sync layer reports a configuration fault instead of signing with a seed.
      signingKey = prefs.getString(KEY_SIGNING_KEY, null) ?: "",
      userName = prefs.getString(KEY_USER_NAME, null) ?: "",
    )

  override fun save(baseUrl: String, signingKey: String, userName: String) {
    prefs.edit {
      putString(KEY_BASE_URL, baseUrl)
      putString(KEY_SIGNING_KEY, signingKey)
      putString(KEY_USER_NAME, userName)
    }
  }

  private companion object {
    const val TAG = "ElisdbSettings"
    const val PREFS_NAME = "elisdb_settings"
    const val KEY_BASE_URL = "server_base_url_override"
    const val KEY_SIGNING_KEY = "signing_key_override"
    const val KEY_USER_NAME = "user_name_override"

    /**
     * Opens the effective settings store, migrating any values written in plain
     * text by older versions of the app.
     *
     * The migration is fail-safe: the legacy values are copied out first, the
     * encrypted store is written and read back to prove the master key works,
     * and only then is the plain-text file cleared. If anything fails — a
     * Keystore that cannot be opened, a write that does not persist — the plain
     * values are restored and the plain store is used, so a broken Keystore
     * keeps the app working instead of losing the signing key or crashing
     * `Application.onCreate`.
     */
    fun openSettings(
      context: Context,
      openEncrypted: (Context) -> SharedPreferences,
    ): SharedPreferences {
      val plain = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val legacyBaseUrl = plain.getString(KEY_BASE_URL, null)
      val legacySigningKey = plain.getString(KEY_SIGNING_KEY, null)
      val legacyUserName = plain.getString(KEY_USER_NAME, null)
      val hasLegacy = legacyBaseUrl != null || legacySigningKey != null || legacyUserName != null
      if (!hasLegacy) {
        return runCatching { openEncrypted(context) }.getOrElse { error ->
          Log.w(TAG, "Encrypted settings unavailable; using the plain-text store", error)
          plain
        }
      }

      // The encrypted store and the legacy one share a file, so the plaintext
      // entries have to go before EncryptedSharedPreferences opens it. The
      // values above are restored if the migration does not complete.
      plain.edit(commit = true) { clear() }
      return try {
        val encrypted = openEncrypted(context)
        // Commit synchronously so the migrated secrets are persisted before the
        // read-back verification.
        encrypted.edit(commit = true) {
          putString(KEY_BASE_URL, legacyBaseUrl ?: "")
          putString(KEY_SIGNING_KEY, legacySigningKey ?: "")
          putString(KEY_USER_NAME, legacyUserName ?: "")
        }
        val migrated =
          encrypted.getString(KEY_BASE_URL, null) == (legacyBaseUrl ?: "") &&
            encrypted.getString(KEY_SIGNING_KEY, null) == (legacySigningKey ?: "") &&
            encrypted.getString(KEY_USER_NAME, null) == (legacyUserName ?: "")
        if (!migrated) error("encrypted settings could not be read back")
        encrypted
      } catch (e: Exception) {
        Log.w(TAG, "Settings migration failed; keeping the plain-text values", e)
        plain.edit(commit = true) {
          legacyBaseUrl?.let { putString(KEY_BASE_URL, it) }
          legacySigningKey?.let { putString(KEY_SIGNING_KEY, it) }
          legacyUserName?.let { putString(KEY_USER_NAME, it) }
        }
        plain
      }
    }

    /**
     * Creates the encrypted preferences. `EncryptedSharedPreferences`/`MasterKey`
     * are deprecated in security-crypto 1.1.0 (Jetpack Security is being
     * retired), but the API is stable and functional. Migrating the seed storage
     * to the Android Keystore or Tink directly is tracked as a separate,
     * deliberate change.
     */
    @Suppress("DEPRECATION")
    fun createEncryptedStore(context: Context): SharedPreferences {
      val masterKey =
        MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
          .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
          .build()
      return EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
      )
    }
  }
}
