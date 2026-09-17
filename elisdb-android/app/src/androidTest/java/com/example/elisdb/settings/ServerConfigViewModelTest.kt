package com.example.elisdb.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.ui.settings.KeyIdentity
import com.example.elisdb.ui.settings.ServerConfigViewModel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.junit.Before
import org.junit.Test

class ServerConfigViewModelTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private lateinit var store: SettingsStore

  private val seed = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="

  private val invalidConfigMessage =
    "Ficheiro de configuração inválido: deve ser JSON com server_url e uma private_seed ed25519 (32 bytes, base64)."

  private fun configJson(url: String = "https://db.example.com", key: String = seed): String =
    """{"server_url":"$url","private_seed":"$key"}"""

  @Before
  fun setUp() {
    context.deleteSharedPreferences("elisdb_settings")
    store = PrefsSettingsStore(context)
  }

  private fun syncManager() =
    SyncManager(
      SyncCoordinatorProvider {
        throw RuntimeException("no network in settings test")
      },
    )

  @Test
  fun import_configPersistsUrlAndSeed() {
    val vm = ServerConfigViewModel(store, syncManager())
    vm.importKey(configJson())
    val saved = store.settings()
    assertEquals("https://db.example.com", saved.baseUrl)
    assertEquals(seed, saved.signingKey)
    assertEquals(true, vm.uiState.value.hasKey)
  }

  @Test
  fun import_bareSeedWithoutUrlRefused() {
    store.save("https://db.example.com", "BEFORE")
    val vm = ServerConfigViewModel(store, syncManager())
    vm.importKey(seed)
    val saved = store.settings()
    assertEquals("BEFORE", saved.signingKey)
    assertEquals("https://db.example.com", saved.baseUrl)
    assertEquals(invalidConfigMessage, vm.uiState.value.signingKeyError)
  }

  @Test
  fun import_issueOutputNoLongerSupported_givesError() {
    store.save("https://db.example.com", "BEFORE")
    val vm = ServerConfigViewModel(store, syncManager())
    val output =
      "issued key for 'Joao' (id issue-abcdef123456)\n" +
        "private seed (import into app): $seed\n" +
        "registered public key:          ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    vm.importKey(output)
    val saved = store.settings()
    assertEquals("BEFORE", saved.signingKey)
    assertEquals(invalidConfigMessage, vm.uiState.value.signingKeyError)
  }

  @Test
  fun import_invalidContentRefusedWithoutOverwrite() {
    store.save("https://db.example.com", "BEFORE")
    val vm = ServerConfigViewModel(store, syncManager())
    vm.importKey("not-a-valid-base64-seed-!!!\nmore lines here")
    val saved = store.settings()
    assertEquals("BEFORE", saved.signingKey)
    assertEquals("https://db.example.com", saved.baseUrl)
    assertEquals(invalidConfigMessage, vm.uiState.value.signingKeyError)
  }

  @Test
  fun import_invalidContentShowsInlineError() {
    val vm = ServerConfigViewModel(store, syncManager())
    vm.importKey("AAAA")
    assertEquals(invalidConfigMessage, vm.uiState.value.signingKeyError)
  }

  @Test
  fun import_invalidSeedInConfigRefused() {
    store.save("https://db.example.com", "BEFORE")
    val vm = ServerConfigViewModel(store, syncManager())
    vm.importKey(configJson(key = "not-a-valid-seed!!!"))
    assertEquals("BEFORE", store.settings().signingKey)
    assertEquals(invalidConfigMessage, vm.uiState.value.signingKeyError)
  }

  @Test
  fun clearConfig_removesUrlKeyAndKeyHidden() {
    store.save("https://db.example.com", seed)
    val vm = ServerConfigViewModel(store, syncManager())
    assertEquals(true, vm.uiState.value.hasKey)
    vm.clearConfig()
    val saved = store.settings()
    assertEquals("", saved.baseUrl)
    assertEquals("", saved.signingKey)
    assertEquals(false, vm.uiState.value.hasKey)
    assertEquals("", vm.uiState.value.baseUrl)
  }

  private fun waitForKeyIdentity(vm: ServerConfigViewModel) {
    val deadline = System.currentTimeMillis() + 5_000
    while (vm.uiState.value.keyIdentity !is KeyIdentity.Resolved && vm.uiState.value.keyIdentity !is KeyIdentity.Unavailable && System.currentTimeMillis() < deadline) {
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      Thread.sleep(50)
    }
  }

  @Test
  fun import_withAssociatedUser_showsUsername() {
    val vm =
      ServerConfigViewModel(
        store,
        syncManager(),
        fetchKeyIdentity = { com.example.elisdb.remote.KeyIdentityDto("key-123", "Joao") },
      )
    vm.importKey(configJson())
    waitForKeyIdentity(vm)
    assertEquals(KeyIdentity.Resolved("Joao"), vm.uiState.value.keyIdentity)
  }

  @Test
  fun import_noAssociatedUser_showsAbsent() {
    val vm =
      ServerConfigViewModel(
        store,
        syncManager(),
        fetchKeyIdentity = { com.example.elisdb.remote.KeyIdentityDto("key-123", null) },
      )
    vm.importKey(configJson())
    waitForKeyIdentity(vm)
    assertEquals(KeyIdentity.Resolved(null), vm.uiState.value.keyIdentity)
  }

  @Test
  fun open_withStoredKey_resolvesUsername() {
    store.save("https://db.example.com", seed)
    val vm =
      ServerConfigViewModel(
        store,
        syncManager(),
        fetchKeyIdentity = { com.example.elisdb.remote.KeyIdentityDto("key-123", "Joao") },
      )
    waitForKeyIdentity(vm)
    assertEquals(KeyIdentity.Resolved("Joao"), vm.uiState.value.keyIdentity)
  }

  @Test
  fun import_lookupFailure_isNonBlocking() {
    val vm =
      ServerConfigViewModel(
        store,
        syncManager(),
        fetchKeyIdentity = {
          throw java.io.IOException("server unreachable")
        },
      )
    vm.importKey(configJson())
    val saved = store.settings()
    assertEquals("https://db.example.com", saved.baseUrl)
    assertEquals(seed, saved.signingKey)
    assertFalse(vm.uiState.value.keyIdentity is KeyIdentity.Resolved)
  }
}
