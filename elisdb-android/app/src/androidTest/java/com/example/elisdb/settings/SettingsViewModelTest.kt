package com.example.elisdb.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.ui.settings.SettingsViewModel
import java.util.concurrent.atomic.AtomicInteger
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CompletableDeferred
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

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
    val vm = SettingsViewModel(store, syncManager())
    vm.importKey(configJson())
    val saved = store.settings()
    assertEquals("https://db.example.com", saved.baseUrl)
    assertEquals(seed, saved.signingKey)
    assertEquals(true, vm.uiState.value.hasKey)
  }

  @Test
  fun import_bareSeedWithoutUrlRefused() {
    store.save("https://db.example.com", "BEFORE")
    val vm = SettingsViewModel(store, syncManager())
    vm.importKey(seed)
    val saved = store.settings()
    assertEquals("BEFORE", saved.signingKey)
    assertEquals("https://db.example.com", saved.baseUrl)
    assertEquals(invalidConfigMessage, vm.uiState.value.signingKeyError)
  }

  @Test
  fun import_issueOutputNoLongerSupported_givesError() {
    store.save("https://db.example.com", "BEFORE")
    val vm = SettingsViewModel(store, syncManager())
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
    val vm = SettingsViewModel(store, syncManager())
    vm.importKey("not-a-valid-base64-seed-!!!\nmore lines here")
    val saved = store.settings()
    assertEquals("BEFORE", saved.signingKey)
    assertEquals("https://db.example.com", saved.baseUrl)
    assertEquals(invalidConfigMessage, vm.uiState.value.signingKeyError)
  }

  @Test
  fun import_invalidContentShowsInlineError() {
    val vm = SettingsViewModel(store, syncManager())
    vm.importKey("AAAA")
    assertEquals(invalidConfigMessage, vm.uiState.value.signingKeyError)
  }

  @Test
  fun import_invalidSeedInConfigRefused() {
    store.save("https://db.example.com", "BEFORE")
    val vm = SettingsViewModel(store, syncManager())
    vm.importKey(configJson(key = "not-a-valid-seed!!!"))
    assertEquals("BEFORE", store.settings().signingKey)
    assertEquals(invalidConfigMessage, vm.uiState.value.signingKeyError)
  }

  @Test
  fun clearConfig_removesUrlKeyAndKeyHidden() {
    store.save("https://db.example.com", seed)
    val vm = SettingsViewModel(store, syncManager())
    assertEquals(true, vm.uiState.value.hasKey)
    vm.clearConfig()
    val saved = store.settings()
    assertEquals("", saved.baseUrl)
    assertEquals("", saved.signingKey)
    assertEquals(false, vm.uiState.value.hasKey)
    assertEquals("", vm.uiState.value.baseUrl)
  }

  @Test
  fun manualSync_triggersSync_andStatusUpdates() {
    val gate = CompletableDeferred<Unit>()
    val manager =
      SyncManager(
        SyncCoordinatorProvider {
          gate.await()
          throw RuntimeException("sync ran")
        },
      )
    val vm = SettingsViewModel(store, manager)
    vm.manualSync()
    waitForStatus(vm, SyncStatus.IN_PROGRESS)
    assertEquals(SyncStatus.IN_PROGRESS, vm.uiState.value.syncStatus)
    gate.complete(Unit)
    waitForSyncFinished(vm)
    assertEquals(SyncStatus.ERROR, vm.uiState.value.syncStatus)
  }

  @Test
  fun manualSync_ignoredWhileAlreadyRunning() {
    val gate = CompletableDeferred<Unit>()
    val calls = AtomicInteger(0)
    val vm =
      SettingsViewModel(
        store,
        SyncManager(
          SyncCoordinatorProvider {
            calls.incrementAndGet()
            gate.await()
            throw RuntimeException("sync ran")
          },
        ),
      )
    vm.manualSync()
    waitForStatus(vm, SyncStatus.IN_PROGRESS)
    vm.manualSync()
    assertEquals(1, calls.get())
    gate.complete(Unit)
    waitForSyncFinished(vm)
    assertEquals(1, calls.get())
  }

  private fun waitForStatus(vm: SettingsViewModel, status: SyncStatus) {
    val deadline = System.currentTimeMillis() + 5_000
    while (vm.uiState.value.syncStatus != status && System.currentTimeMillis() < deadline) {
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      Thread.sleep(50)
    }
  }

  private fun waitForSyncFinished(vm: SettingsViewModel) {
    val deadline = System.currentTimeMillis() + 5_000
    while (vm.uiState.value.syncStatus == SyncStatus.IN_PROGRESS && System.currentTimeMillis() < deadline) {
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      Thread.sleep(50)
    }
  }

  private fun waitForKeyIdentity(vm: SettingsViewModel) {
    val deadline = System.currentTimeMillis() + 5_000
    while (vm.uiState.value.keyIdentity !is com.example.elisdb.ui.settings.KeyIdentity.Resolved && vm.uiState.value.keyIdentity !is com.example.elisdb.ui.settings.KeyIdentity.Unavailable && System.currentTimeMillis() < deadline) {
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      Thread.sleep(50)
    }
  }

  @Test
  fun import_withAssociatedUser_showsUsername() {
    val vm =
      SettingsViewModel(
        store,
        syncManager(),
        fetchKeyIdentity = { com.example.elisdb.remote.KeyIdentityDto("key-123", "Joao") },
      )
    vm.importKey(configJson())
    waitForKeyIdentity(vm)
    assertEquals(
      com.example.elisdb.ui.settings.KeyIdentity.Resolved("Joao"),
      vm.uiState.value.keyIdentity,
    )
  }

  @Test
  fun import_noAssociatedUser_showsAbsent() {
    val vm =
      SettingsViewModel(
        store,
        syncManager(),
        fetchKeyIdentity = { com.example.elisdb.remote.KeyIdentityDto("key-123", null) },
      )
    vm.importKey(configJson())
    waitForKeyIdentity(vm)
    assertEquals(
      com.example.elisdb.ui.settings.KeyIdentity.Resolved(null),
      vm.uiState.value.keyIdentity,
    )
  }

  @Test
  fun open_withStoredKey_resolvesUsername() {
    store.save("https://db.example.com", seed)
    val vm =
      SettingsViewModel(
        store,
        syncManager(),
        fetchKeyIdentity = { com.example.elisdb.remote.KeyIdentityDto("key-123", "Joao") },
      )
    waitForKeyIdentity(vm)
    assertEquals(
      com.example.elisdb.ui.settings.KeyIdentity.Resolved("Joao"),
      vm.uiState.value.keyIdentity,
    )
  }

  @Test
  fun import_lookupFailure_isNonBlocking() {
    val vm =
      SettingsViewModel(
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
    assertFalse(vm.uiState.value.keyIdentity is com.example.elisdb.ui.settings.KeyIdentity.Resolved)
  }

  @Test
  fun pendingUploads_saoExpotasNoUiState() {
    val dao = FakeAndroidClienteDao()
    val manager = pendingSyncManager(dao = dao)
    val vm = SettingsViewModel(store, manager)
    val pending = Cliente(id = "1", nome = "A", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0, pending = true)
    dao.add(pending)
    // allow the collector a tick
    Thread.sleep(200)
    assertTrue(vm.uiState.value.hasPendingUploads)
  }

  @Test
  fun semPendencias_naoMarcaUploads() {
    val dao = FakeAndroidClienteDao()
    val manager = pendingSyncManager(dao = dao)
    val vm = SettingsViewModel(store, manager)
    Thread.sleep(200)
    assertFalse(vm.uiState.value.hasPendingUploads)
  }

  @Test
  fun pendingDownloads_saoExpotasNoUiState() {
    val watermark = FakeAndroidPullWatermark(100L)
    val manager = pendingSyncManager(watermark = watermark, latest = { 500L })
    val vm = SettingsViewModel(store, manager)
    vm.refreshPendingState()
    Thread.sleep(200)
    assertTrue(vm.uiState.value.hasPendingDownloads)
  }

  private fun pendingSyncManager(
    dao: FakeAndroidClienteDao = FakeAndroidClienteDao(),
    watermark: FakeAndroidPullWatermark = FakeAndroidPullWatermark(null),
    latest: () -> Long? = { null },
  ) =
    SyncManager(
      SyncCoordinatorProvider {
        throw RuntimeException("no network in settings test")
      },
      clienteDao = dao,
      pullWatermark = watermark,
      fetchLatestUpdatedAt = latest,
    )
}

private class FakeAndroidClienteDao : com.example.elisdb.data.local.ClienteDao {
  private val clients = kotlinx.coroutines.flow.MutableStateFlow<List<Cliente>>(emptyList())
  fun add(c: Cliente) {
    clients.value = clients.value + c
  }

  override fun observeAll(): kotlinx.coroutines.flow.Flow<List<Cliente>> = clients

  override fun observeAllIncludingDeleted(): kotlinx.coroutines.flow.Flow<List<Cliente>> = clients

  override fun observeAllComFotos(): kotlinx.coroutines.flow.Flow<List<com.example.elisdb.data.local.ClienteComFotos>> =
    kotlinx.coroutines.flow.flowOf(emptyList())

  override fun observeClienteComFotos(id: String): kotlinx.coroutines.flow.Flow<com.example.elisdb.data.local.ClienteComFotos?> =
    kotlinx.coroutines.flow.flowOf(null)

  override suspend fun getAll(): List<Cliente> = clients.value
  override suspend fun getClienteComFotos(id: String): com.example.elisdb.data.local.ClienteComFotos? = null
  override suspend fun getById(id: String): Cliente? = clients.value.find { it.id == id }
  override suspend fun getPending(): List<Cliente> = clients.value.filter { it.pending }
  override suspend fun getPendingDeletions(): List<Cliente> = clients.value.filter { it.deletePending }
  override suspend fun insert(cliente: Cliente) = Unit
  override suspend fun upsert(cliente: Cliente) = Unit
  override suspend fun update(cliente: Cliente) = Unit
  override suspend fun delete(cliente: Cliente) = Unit
}

private class FakeAndroidPullWatermark(var value: Long?) : com.example.elisdb.sync.PullWatermark {
  override suspend fun get(): Long? = value
  override suspend fun set(value: Long) {
    this.value = value
  }
}