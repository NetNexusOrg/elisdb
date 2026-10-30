package com.example.elisdb.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.remote.ApiClientProvider
import com.example.elisdb.remote.ConfigException
import com.example.elisdb.util.Signer
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import org.junit.Before
import org.junit.Test

class SettingsStoreTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  fun setUp() {
    context.deleteSharedPreferences("elisdb_settings")
  }

  @Test
  fun settings_reflectsBuildConfigDefaultsWhenNothingSaved() {
    val store = PrefsSettingsStore(context)
    val settings = store.settings()
    assertEquals(com.example.elisdb.BuildConfig.SERVER_BASE_URL, settings.baseUrl)
    // The signing key must come from a per-device import, never from a build-time
    // constant, so nothing saved means a blank key (not the embedded seed).
    assertEquals("", settings.signingKey)
  }

  @Test
  fun save_persistsOverridesAndOverrideIsReadBack() {
    val store = PrefsSettingsStore(context)
    store.save("https://db.example.com", "abc123")
    val settings = store.settings()
    assertEquals("https://db.example.com", settings.baseUrl)
    assertEquals("abc123", settings.signingKey)
  }

  @Test
  fun save_blankValuesPersistedVerbatim() {
    val store = PrefsSettingsStore(context)
    store.save("", "")
    val settings = store.settings()
    assertEquals("", settings.baseUrl)
    assertEquals("", settings.signingKey)
  }

  @Test
  fun provider_blankResolvedConfigRaisesConfigException() {
    val store = PrefsSettingsStore(context)
    store.save("", "")
    try {
      ApiClientProvider.create(store)
      fail("expected ConfigException")
    } catch (expected: ConfigException) {
      // expected
    }
  }

  @Test
  fun import_validServerIssuedSeedPersistsOverride() {
    val store = PrefsSettingsStore(context)
    val seed = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
    store.save("https://db.example.com", seed)
    val settings = store.settings()
    assertEquals("https://db.example.com", settings.baseUrl)
    assertEquals(seed, settings.signingKey)
  }

  @Test
  fun provider_usesImportedSeedForSigning() {
    val store = PrefsSettingsStore(context)
    val seed = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
    store.save("https://db.example.com", seed)
    val api = ApiClientProvider.create(store)
    val signature =
      api.signer.signRequest(
        "GET",
        "/api/pull",
        ByteArray(0),
        timestamp = 1700000000L,
      )
    val expected = Signer.fromBase64(seed).signRequest("GET", "/api/pull", ByteArray(0), 1700000000L)
    assertEquals(expected, signature)
  }

  @Test
  fun legacyPlainValuesAreMigratedToEncryptedStore() {
    // Simulate a pre-upgrade version that stored the key in plain text.
    val plain = context.getSharedPreferences("elisdb_settings", Context.MODE_PRIVATE)
    plain.edit().putString("server_base_url_override", "https://legacy.example.com")
      .putString("signing_key_override", "LEGACYSEED")
      .commit()

    val store = PrefsSettingsStore(context)
    val settings = store.settings()
    assertEquals("https://legacy.example.com", settings.baseUrl)
    assertEquals("LEGACYSEED", settings.signingKey)

    // The plain-text file is cleared after migration (no plaintext key left).
    val after = context.getSharedPreferences("elisdb_settings", Context.MODE_PRIVATE)
    assertEquals(null, after.getString("signing_key_override", null))
  }

  @Test
  fun legacyMigrationKeepsPlainValuesWhenEncryptedStoreFails() {
    val plain = context.getSharedPreferences("elisdb_settings", Context.MODE_PRIVATE)
    plain.edit().putString("server_base_url_override", "https://legacy.example.com")
      .putString("signing_key_override", "LEGACYSEED")
      .putString("user_name_override", "maria")
      .commit()

    // A device with a broken Keystore fails to open the encrypted store. The
    // migration must not lose a working configuration: it falls back to the
    // plain file and leaves it untouched.
    val store =
      PrefsSettingsStore(context, openEncrypted = { throw IllegalStateException("keystore unavailable") })

    val settings = store.settings()
    assertEquals("https://legacy.example.com", settings.baseUrl)
    assertEquals("LEGACYSEED", settings.signingKey)
    assertEquals("maria", settings.userName)

    val after = context.getSharedPreferences("elisdb_settings", Context.MODE_PRIVATE)
    assertEquals("https://legacy.example.com", after.getString("server_base_url_override", null))
    assertEquals("LEGACYSEED", after.getString("signing_key_override", null))
    assertEquals("maria", after.getString("user_name_override", null))
  }

  @Test
  fun savesGoToThePlainFallbackWhenEncryptedStoreFails() {
    val store =
      PrefsSettingsStore(context, openEncrypted = { throw IllegalStateException("keystore unavailable") })
    store.save("https://fallback.example.com", "SEED", "maria")

    assertEquals("https://fallback.example.com", store.settings().baseUrl)
    val plain = context.getSharedPreferences("elisdb_settings", Context.MODE_PRIVATE)
    assertEquals("https://fallback.example.com", plain.getString("server_base_url_override", null))
  }
}