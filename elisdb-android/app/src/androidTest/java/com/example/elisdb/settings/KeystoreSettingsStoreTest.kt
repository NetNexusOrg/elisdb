package com.example.elisdb.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.security.KeyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeystoreSettingsStoreTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    clearPrefs(SECURE_PREFS)
    clearPrefs(LEGACY_PREFS)
    deleteKeyAlias()
  }

  @After
  fun tearDown() {
    clearPrefs(SECURE_PREFS)
    clearPrefs(LEGACY_PREFS)
    deleteKeyAlias()
  }

  @Test
  fun saveAndReadRoundTrip() {
    val store = KeystoreSettingsStore(context)

    store.save("https://server.example.com", SEED, "maria")

    val settings = store.settings()
    assertEquals("https://server.example.com", settings.baseUrl)
    assertEquals(SEED, settings.signingKey)
    assertEquals("maria", settings.userName)
  }

  @Test
  fun securePrefsDoNotExposeTheSeed() {
    KeystoreSettingsStore(context).save("https://server.example.com", SEED, "maria")

    val raw = context.getSharedPreferences(SECURE_PREFS, Context.MODE_PRIVATE)
    val stored = raw.all.values.joinToString(" ")
    assertFalse(stored.contains(SEED))
    assertTrue(stored.contains("v1:"))
  }

  @Test
  fun migratesLegacyPlaintextSettings() {
    val plain = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
    plain.edit()
      .putString(BASE_URL_KEY, "https://old.example.com")
      .putString(SIGNING_KEY_KEY, SEED)
      .putString(USER_NAME_KEY, "maria")
      .commit()

    val settings = KeystoreSettingsStore(context).settings()

    assertEquals("https://old.example.com", settings.baseUrl)
    assertEquals(SEED, settings.signingKey)
    assertEquals("maria", settings.userName)
    assertFalse(plain.contains(SIGNING_KEY_KEY))
  }

  @Test
  fun migratesLegacyEncryptedSettings() {
    PrefsSettingsStore(context).save("https://legacy.example.com", SEED, "maria")

    val settings = KeystoreSettingsStore(context).settings()

    assertEquals("https://legacy.example.com", settings.baseUrl)
    assertEquals(SEED, settings.signingKey)
    assertEquals("maria", settings.userName)
  }

  @Test
  fun keepsLegacyValuesWhenKeystoreIsUnavailable() {
    val plain = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
    plain.edit()
      .putString(BASE_URL_KEY, "https://old.example.com")
      .putString(SIGNING_KEY_KEY, SEED)
      .putString(USER_NAME_KEY, "maria")
      .commit()
    val store = KeystoreSettingsStore(context) {
      throw IllegalStateException("Keystore unavailable")
    }

    assertEquals(SEED, store.settings().signingKey)
    assertThrows(SecureStorageUnavailable::class.java) {
      store.save("https://new.example.com", SEED, "maria")
    }
    assertEquals(SEED, plain.getString(SIGNING_KEY_KEY, null))
  }

  private fun clearPrefs(name: String) {
    context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
  }

  private fun deleteKeyAlias() {
    runCatching {
      val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
      keyStore.deleteEntry(KEY_ALIAS)
    }
  }

  private companion object {
    const val SECURE_PREFS = "elisdb_settings_keystore"
    const val LEGACY_PREFS = "elisdb_settings"
    const val BASE_URL_KEY = "server_base_url_override"
    const val SIGNING_KEY_KEY = "signing_key_override"
    const val USER_NAME_KEY = "user_name_override"
    const val KEY_ALIAS = "elisdb_settings_aes_v1"
    const val SEED = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
  }
}
