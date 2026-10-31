package com.example.elisdb

import com.example.elisdb.settings.ServerSettings
import com.example.elisdb.settings.SettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ElisdbAppTest {
  @Test
  fun initialSyncRunsWhenAServerIsConfigured() = runTest {
    var syncs = 0
    initialSyncIfConfigured(
      settingsStore = FakeSettingsStore(ServerSettings(baseUrl = "https://server", signingKey = "key")),
      syncNow = { syncs++ },
    )

    assertEquals(1, syncs)
  }

  @Test
  fun initialSyncIsSkippedWithoutASigningKey() = runTest {
    var syncs = 0
    initialSyncIfConfigured(
      settingsStore = FakeSettingsStore(ServerSettings(baseUrl = "https://server", signingKey = "")),
      syncNow = { syncs++ },
    )

    assertEquals(0, syncs)
  }

  private class FakeSettingsStore(private var current: ServerSettings) : SettingsStore {
    override fun settings(): ServerSettings = current

    override fun save(baseUrl: String, signingKey: String, userName: String) {
      current = ServerSettings(baseUrl, signingKey, userName)
    }
  }
}
