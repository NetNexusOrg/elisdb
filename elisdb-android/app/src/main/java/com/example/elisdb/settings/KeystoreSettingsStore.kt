package com.example.elisdb.settings

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.elisdb.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the server settings encrypted with an AES/GCM key that only Android
 * Keystore can use. It replaces the deprecated `androidx.security:security-crypto`
 * store while migrating both its data and the older plaintext format.
 *
 * The migration is deliberately fail-safe: legacy values are kept readable until
 * the new store has been written and read back successfully. If Keystore is
 * unavailable, reads fall back to the legacy data and writes fail closed instead
 * of writing the signing key in plaintext.
 */
class KeystoreSettingsStore(
  context: Context,
  private val keyProvider: () -> SecretKey = { loadOrCreateKey() },
) : SettingsStore {
  private val appContext = context.applicationContext
  private val secure: SharedPreferences =
    appContext.getSharedPreferences(SECURE_PREFS, Context.MODE_PRIVATE)
  private val legacyPlain: SharedPreferences =
    appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)

  @Volatile
  private var unavailableError: Throwable? = null

  init {
    migrateLegacy()
  }

  override fun settings(): ServerSettings {
    val stored = readSecure() ?: readLegacy()
    return ServerSettings(
      baseUrl = stored?.baseUrl?.takeIf { it.isNotEmpty() } ?: BuildConfig.SERVER_BASE_URL,
      signingKey = stored?.signingKey.orEmpty(),
      userName = stored?.userName.orEmpty(),
    )
  }

  override fun save(baseUrl: String, signingKey: String, userName: String) {
    try {
      writeSecure(StoredSettings(baseUrl, signingKey, userName))
    } catch (e: Exception) {
      unavailableError = e
      Log.w(TAG, "could not write settings with Keystore", e)
      throw SecureStorageUnavailable(
        "Não foi possível guardar a configuração com segurança.",
        e,
      )
    }
    // Only after the new values were verified do we drop the legacy copies.
    clearLegacy()
  }

  private fun readSecure(): StoredSettings? {
    if (!secure.containsAny()) return null
    return try {
      StoredSettings(
        baseUrl = decrypt(secure.getString(BASE_URL_KEY, null)),
        signingKey = decrypt(secure.getString(SIGNING_KEY_KEY, null)),
        userName = decrypt(secure.getString(USER_NAME_KEY, null)),
      )
    } catch (e: Exception) {
      unavailableError = e
      Log.w(TAG, "could not read settings from Keystore", e)
      null
    }
  }

  private fun writeSecure(values: StoredSettings) {
    val editor = secure.edit()
      .putString(BASE_URL_KEY, encrypt(values.baseUrl))
      .putString(SIGNING_KEY_KEY, encrypt(values.signingKey))
      .putString(USER_NAME_KEY, encrypt(values.userName))
    if (!editor.commit()) error("Keystore settings commit failed")
    check(readSecure() == values) { "Keystore settings read-back mismatch" }
  }

  private fun readLegacy(): StoredSettings? {
    // Very old builds stored plaintext values in the same file; that copy wins
    // until it is migrated. The encrypted store uses encrypted key names, so
    // both can coexist in the file during the upgrade.
    legacyPlain.readValues()?.let { return it }
    return runCatching { openLegacyEncrypted().readValues() }.getOrNull()
  }

  private fun migrateLegacy() {
    if (secure.containsAny()) return
    val legacy = readLegacy() ?: return
    try {
      writeSecure(legacy)
      clearLegacy()
    } catch (e: Exception) {
      // Keep the legacy values intact so the user can still use the app (and
      // reimport the credential if this device's Keystore is broken).
      unavailableError = e
      Log.w(TAG, "could not migrate settings to Keystore", e)
    }
  }

  private fun clearLegacy() {
    if (legacyPlain.containsAny()) {
      legacyPlain.edit()
        .remove(BASE_URL_KEY)
        .remove(SIGNING_KEY_KEY)
        .remove(USER_NAME_KEY)
        .commit()
    }
    runCatching {
      val legacy = openLegacyEncrypted()
      if (legacy.containsAny()) legacy.edit().clear().commit()
    }
  }

  @Suppress("DEPRECATION")
  private fun openLegacyEncrypted(): SharedPreferences {
    val masterKey = MasterKey.Builder(appContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()
    return EncryptedSharedPreferences.create(
      appContext,
      LEGACY_PREFS,
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
  }

  private fun encrypt(value: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
    val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    return "$FORMAT_VERSION:${encode(cipher.iv)}:${encode(ciphertext)}"
  }

  private fun decrypt(stored: String?): String {
    if (stored == null) return ""
    val parts = stored.split(":", limit = 3)
    require(parts.size == 3 && parts[0] == FORMAT_VERSION) { "unsupported secured value" }
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(
      Cipher.DECRYPT_MODE,
      keyProvider(),
      GCMParameterSpec(GCM_TAG_BITS, decode(parts[1])),
    )
    return String(cipher.doFinal(decode(parts[2])), Charsets.UTF_8)
  }

  private fun encode(bytes: ByteArray): String =
    Base64.encodeToString(bytes, Base64.NO_WRAP)

  private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

  private data class StoredSettings(
    val baseUrl: String,
    val signingKey: String,
    val userName: String,
  )

  private fun SharedPreferences.containsAny(): Boolean =
    contains(BASE_URL_KEY) || contains(SIGNING_KEY_KEY) || contains(USER_NAME_KEY)

  private fun SharedPreferences.readValues(): StoredSettings? {
    if (!containsAny()) return null
    return StoredSettings(
      baseUrl = getString(BASE_URL_KEY, null).orEmpty(),
      signingKey = getString(SIGNING_KEY_KEY, null).orEmpty(),
      userName = getString(USER_NAME_KEY, null).orEmpty(),
    )
  }

  private companion object {
    const val TAG = "ElisdbSettings"
    const val SECURE_PREFS = "elisdb_settings_keystore"
    const val LEGACY_PREFS = "elisdb_settings"
    const val BASE_URL_KEY = "server_base_url_override"
    const val SIGNING_KEY_KEY = "signing_key_override"
    const val USER_NAME_KEY = "user_name_override"
    const val KEY_ALIAS = "elisdb_settings_aes_v1"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_TAG_BITS = 128
    const val FORMAT_VERSION = "v1"

    fun loadOrCreateKey(): SecretKey {
      val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
      (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
        return it.secretKey
      }
      val generator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        "AndroidKeyStore",
      )
      generator.init(
        KeyGenParameterSpec.Builder(
          KEY_ALIAS,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setKeySize(256)
          .build(),
      )
      return generator.generateKey()
    }
  }
}
