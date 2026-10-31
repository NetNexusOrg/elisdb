package com.example.elisdb.remote

import com.example.elisdb.settings.ServerSettings
import com.example.elisdb.settings.SettingsStore
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientProviderTest {

  private val seed = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="

  private class FakeSettingsStore(private val current: ServerSettings) : SettingsStore {
    override fun settings(): ServerSettings = current

    override fun save(baseUrl: String, signingKey: String, userName: String) = Unit
  }

  @Test
  fun `public http server url is rejected with a clear message`() {
    val store = FakeSettingsStore(ServerSettings("http://db.example.com", seed))

    try {
      ApiClientProvider.create(store)
      throw AssertionError("expected ConfigException")
    } catch (e: ConfigException) {
      assertTrue("mentions public HTTP", e.message?.contains("HTTP público") == true)
    }
  }
}
