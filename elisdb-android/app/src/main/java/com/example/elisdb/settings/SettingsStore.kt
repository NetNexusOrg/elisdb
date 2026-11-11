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

/**
 * True when the app has a server URL and a signing key, i.e. it can authenticate
 * against the server. The user name is not required for the connection.
 */
fun ServerSettings.hasCredentials(): Boolean =
  baseUrl.isNotBlank() && signingKey.isNotBlank()

/**
 * True when the app has a complete user configuration: server, signing key and
 * the user name carried by the imported credential. The app only operates with
 * a complete configuration; see `ServerSettings.recoverUserName`.
 */
fun ServerSettings.hasConfiguredUser(): Boolean =
  hasCredentials() && userName.isNotBlank()

/** Storage for runtime server connection settings (base URL + signing key). */
interface SettingsStore {
  /** Currently effective values: runtime override when saved, else build-time default. */
  fun settings(): ServerSettings

  /**
   * Persists runtime overrides. Blank values are stored verbatim.
   *
   * @throws SecureStorageUnavailable when the encrypted store cannot be opened or
   *   a write cannot be verified. The app never falls back to writing the
   *   signing key in plain text.
   */
  fun save(baseUrl: String, signingKey: String, userName: String = "")
}

/** Raised when a signing-key write cannot be completed securely. */
class SecureStorageUnavailable(message: String, cause: Throwable? = null) :
  IllegalStateException(message, cause)

/**
 * Stores the runtime server connection overrides (base URL + signing key seed)
 * encrypted at rest via [EncryptedSharedPreferences] (master key in the Android
 * Keystore). Values saved by older plain-text versions are migrated on first use
 * and the plain-text file is cleared only after the encrypted copy has been read
 * back successfully.
 *
 * Reads remain fail-open for legacy plain-text data so an upgraded app still
 * starts when the Keystore is temporarily unavailable. Writes are fail-closed:
 * [save] throws [SecureStorageUnavailable] instead of storing the key in plain
 * text.
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
  private val plain: SharedPreferences =
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private var encrypted: SharedPreferences? = null
  private var encryptedUnavailable = false
  private var encryptedError: Throwable? = null

  init {
    migrateLegacy(openEncrypted)
  }

  override fun settings(): ServerSettings {
    val store = encryptedOrNull() ?: plain
    return ServerSettings(
      baseUrl = store.getString(KEY_BASE_URL, null) ?: BuildConfig.SERVER_BASE_URL,
      // The signing key is never taken from a build-time constant: it must come
      // from a per-device import, so a key baked into the APK can never be used
      // to authenticate. When no key has been imported, this is blank and the
      // sync layer reports a configuration fault instead of signing with a seed.
      signingKey = store.getString(KEY_SIGNING_KEY, null) ?: "",
      userName = store.getString(KEY_USER_NAME, null) ?: "",
    )
  }

  override fun save(baseUrl: String, signingKey: String, userName: String) {
    val store =
      encryptedOrNull()
        ?: throw SecureStorageUnavailable(
          "O armazenamento seguro não está disponível; a chave não foi guardada.",
          encryptedError,
        )
    store.edit(commit = true) {
      putString(KEY_BASE_URL, baseUrl)
      putString(KEY_SIGNING_KEY, signingKey)
      putString(KEY_USER_NAME, userName)
    }
    val verified =
      store.getString(KEY_BASE_URL, null) == baseUrl &&
        store.getString(KEY_SIGNING_KEY, null) == signingKey &&
        store.getString(KEY_USER_NAME, null) == userName
    if (!verified) {
      throw SecureStorageUnavailable(
        "Não foi possível confirmar a gravação segura da chave.",
        encryptedError,
      )
    }
  }

  private fun encryptedOrNull(): SharedPreferences? {
    encrypted?.let { return it }
    if (encryptedUnavailable) return null
    return runCatching {
        migrateLegacy(::createEncryptedStore)
        checkNotNull(encrypted) { "encrypted settings are unavailable" }
      }
      .getOrElse { error ->
        encryptedUnavailable = true
        encryptedError = error
        Log.w(TAG, "Encrypted settings unavailable; refusing to write secrets in plain text", error)
        null
      }
  }

  private fun migrateLegacy(openEncrypted: (Context) -> SharedPreferences) {
    val legacyBaseUrl = plain.getString(KEY_BASE_URL, null)
    val legacySigningKey = plain.getString(KEY_SIGNING_KEY, null)
    val legacyUserName = plain.getString(KEY_USER_NAME, null)
    val hasLegacy = legacyBaseUrl != null || legacySigningKey != null || legacyUserName != null

    val opened =
      try {
        openEncrypted(appContext)
      } catch (error: Exception) {
        encryptedUnavailable = true
        encryptedError = error
        Log.w(TAG, "Encrypted settings unavailable; using legacy read-only values", error)
        return
      }

    if (!hasLegacy) {
      encrypted = opened
      return
    }

    // The encrypted store and the legacy one share a file, so migrate the
    // values and prove they can be read back before clearing the plaintext.
    try {
      opened.edit(commit = true) {
        putString(KEY_BASE_URL, legacyBaseUrl ?: "")
        putString(KEY_SIGNING_KEY, legacySigningKey ?: "")
        putString(KEY_USER_NAME, legacyUserName ?: "")
      }
      val migrated =
        opened.getString(KEY_BASE_URL, null) == (legacyBaseUrl ?: "") &&
          opened.getString(KEY_SIGNING_KEY, null) == (legacySigningKey ?: "") &&
          opened.getString(KEY_USER_NAME, null) == (legacyUserName ?: "")
      if (!migrated) error("encrypted settings could not be read back")
      // The encrypted store and the legacy one share a file, so remove only
      // the plaintext keys after the encrypted copy is verified. Clearing the
      // whole file would also delete the encrypted entries just written.
      plain.edit(commit = true) {
        remove(KEY_BASE_URL)
        remove(KEY_SIGNING_KEY)
        remove(KEY_USER_NAME)
      }
      encrypted = opened
    } catch (error: Exception) {
      encryptedUnavailable = true
      encryptedError = error
      Log.w(TAG, "Settings migration failed; keeping legacy values read-only", error)
    }
  }

  private companion object {
    const val TAG = "ElisdbSettings"
    const val PREFS_NAME = "elisdb_settings"
    const val KEY_BASE_URL = "server_base_url_override"
    const val KEY_SIGNING_KEY = "signing_key_override"
    const val KEY_USER_NAME = "user_name_override"

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
