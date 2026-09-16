package com.example.elisdb.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.elisdb.BuildConfig

/** Effective server connection settings for the app. */
data class ServerSettings(
  val baseUrl: String,
  val signingKey: String,
)

/** Storage for runtime server connection settings (base URL + signing key). */
interface SettingsStore {
  /** Currently effective values: runtime override when saved, else build-time default. */
  fun settings(): ServerSettings

  /** Persists runtime overrides. Blank values are stored verbatim. */
  fun save(baseUrl: String, signingKey: String)
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
class PrefsSettingsStore(context: Context) : SettingsStore {
  private val appContext = context.applicationContext
  private val prefs: SharedPreferences = createEncryptedPrefs(appContext)

  override fun settings(): ServerSettings =
    ServerSettings(
      baseUrl = prefs.getString(KEY_BASE_URL, null) ?: BuildConfig.SERVER_BASE_URL,
      // The signing key is never taken from a build-time constant: it must come
      // from a per-device import, so a key baked into the APK can never be used
      // to authenticate. When no key has been imported, this is blank and the
      // sync layer reports a configuration fault instead of signing with a seed.
      signingKey = prefs.getString(KEY_SIGNING_KEY, null) ?: "",
    )

  override fun save(baseUrl: String, signingKey: String) {
    prefs.edit {
      putString(KEY_BASE_URL, baseUrl)
      putString(KEY_SIGNING_KEY, signingKey)
    }
  }

  private companion object {
    const val PREFS_NAME = "elisdb_settings"
    const val KEY_BASE_URL = "server_base_url_override"
    const val KEY_SIGNING_KEY = "signing_key_override"

    /**
     * Creates the encrypted preferences, migrating any values stored in plain
     * text by older versions of the app. The plain file is read first, then
     * cleared, so the encrypted store never has to decode raw plaintext.
     */
    fun createEncryptedPrefs(context: Context): SharedPreferences {
      val plainPrefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val legacyBaseUrl = plainPrefs.getString(KEY_BASE_URL, null)
      val legacySigningKey = plainPrefs.getString(KEY_SIGNING_KEY, null)
      if (legacyBaseUrl != null || legacySigningKey != null) {
        // Commit synchronously so the plaintext file is cleared before returning.
        plainPrefs.edit(commit = true) { clear() }
      }

      val masterKey =
        MasterKey.Builder(context)
          .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
          .build()
      val encrypted =
        EncryptedSharedPreferences.create(
          context,
          PREFS_NAME,
          masterKey,
          EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
          EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
      if (legacyBaseUrl != null || legacySigningKey != null) {
        // Commit synchronously so the migrated secrets are persisted before returning.
        encrypted.edit(commit = true) {
          putString(KEY_BASE_URL, legacyBaseUrl ?: "")
          putString(KEY_SIGNING_KEY, legacySigningKey ?: "")
        }
      }
      return encrypted
    }
  }
}
