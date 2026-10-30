package com.example.elisdb.ui.settings

import com.example.elisdb.remote.KeyIdentityDto
import com.example.elisdb.settings.ServerSettings
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.sync.FakeClienteDao
import com.example.elisdb.sync.FakeFotoDao
import com.example.elisdb.sync.FakePhotoStore
import com.example.elisdb.sync.FakeSyncApi
import com.example.elisdb.sync.SyncCoordinator
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerConfigViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun syncManager(runScope: kotlinx.coroutines.CoroutineScope): SyncManager =
    SyncManager(
      SyncCoordinatorProvider {
        SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore())
      },
      runScope = runScope,
    )

  private fun vm(
    store: SettingsStore,
    runScope: kotlinx.coroutines.CoroutineScope,
    fetch: suspend () -> KeyIdentityDto? = { null },
  ): ServerConfigViewModel = ServerConfigViewModel(store, syncManager(runScope), fetch)

  @Test
  fun `sem chave mantem identidade nao resolvida`() = runTest(dispatcher) {
    val viewModel = vm(FakeSettingsStore(ServerSettings("https://s", "")), backgroundScope)
    advanceUntilIdle()
    assertEquals(KeyIdentity.Unresolved, viewModel.uiState.value.keyIdentity)
  }

  @Test
  fun `resolving mostrado durante o lookup`() = runTest(dispatcher) {
    val gate = CompletableDeferred<Unit>()
    val viewModel =
      vm(
        FakeSettingsStore(ServerSettings("https://s", "seed")),
        backgroundScope,
        fetch = {
          gate.await()
          KeyIdentityDto("k1", "maria")
        },
      )
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.keyIdentity is KeyIdentity.Resolving)
    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(KeyIdentity.Resolved("maria"), viewModel.uiState.value.keyIdentity)
  }

  @Test
  fun `resolved com nome`() = runTest(dispatcher) {
    val viewModel =
      vm(
        FakeSettingsStore(ServerSettings("https://s", "seed")),
        backgroundScope,
        fetch = { KeyIdentityDto("k1", "maria") },
      )
    advanceUntilIdle()
    assertEquals(KeyIdentity.Resolved("maria"), viewModel.uiState.value.keyIdentity)
  }

  @Test
  fun `resolved sem nome quando servidor responde sem utilizador`() = runTest(dispatcher) {
    val viewModel =
      vm(
        FakeSettingsStore(ServerSettings("https://s", "seed")),
        backgroundScope,
        fetch = { KeyIdentityDto("k1", null) },
      )
    advanceUntilIdle()
    assertEquals(KeyIdentity.Resolved(null), viewModel.uiState.value.keyIdentity)
  }

  @Test
  fun `falha do lookup da unavailable`() = runTest(dispatcher) {
    val viewModel =
      vm(
        FakeSettingsStore(ServerSettings("https://s", "seed")),
        backgroundScope,
        fetch = { null },
      )
    advanceUntilIdle()
    assertEquals(KeyIdentity.Unavailable, viewModel.uiState.value.keyIdentity)
  }

  @Test
  fun `sync bem sucedido re-resolve identidade indisponivel`() = runTest(dispatcher) {
    var calls = 0
    val manager = syncManager(backgroundScope)
    val viewModel =
      ServerConfigViewModel(
        FakeSettingsStore(ServerSettings("https://s", "seed")),
        manager,
        fetchKeyIdentity = {
          calls++
          if (calls == 1) null else KeyIdentityDto("k1", "maria")
        },
      )
    advanceUntilIdle()
    assertEquals(KeyIdentity.Unavailable, viewModel.uiState.value.keyIdentity)
    assertEquals(1, calls)
    manager.syncNow()
    advanceUntilIdle()
    assertEquals(SyncStatus.SUCCESS, manager.status.value)
    assertEquals(KeyIdentity.Resolved("maria"), viewModel.uiState.value.keyIdentity)
  }

  @Test
  fun `nome do ficheiro e mostrado sem consultar o servidor`() = runTest(dispatcher) {
    var calls = 0
    val viewModel =
      vm(
        FakeSettingsStore(ServerSettings("https://s", "seed", "maria")),
        backgroundScope,
        fetch = {
          calls++
          KeyIdentityDto("k1", "outro")
        },
      )
    advanceUntilIdle()
    assertEquals(KeyIdentity.Resolved("maria"), viewModel.uiState.value.keyIdentity)
    assertEquals(0, calls)
  }

  @Test
  fun `importar configuracao guarda o nome do utilizador`() = runTest(dispatcher) {
    var calls = 0
    val store = FakeSettingsStore(ServerSettings("", ""))
    val viewModel =
      vm(
        store,
        backgroundScope,
        fetch = {
          calls++
          null
        },
      )
    viewModel.importKey(
      """{"server_url":"https://s","private_seed":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","user_name":"carlos"}""",
    )
    advanceUntilIdle()
    assertEquals(KeyIdentity.Resolved("carlos"), viewModel.uiState.value.keyIdentity)
    assertEquals("carlos", store.current.userName)
    assertEquals(0, calls)
  }

  @Test
  fun `clearConfig limpa url chave e utilizador`() = runTest(dispatcher) {
    val store = FakeSettingsStore(ServerSettings("https://s", "seed"))
    val viewModel =
      vm(
        store,
        backgroundScope,
        fetch = { KeyIdentityDto("k1", "maria") },
      )
    advanceUntilIdle()
    assertEquals("https://s", viewModel.uiState.value.baseUrl)
    assertEquals(true, viewModel.uiState.value.hasKey)

    viewModel.clearConfig()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.baseUrl)
    assertEquals("", state.signingKey)
    assertEquals(false, state.hasKey)
    assertEquals(KeyIdentity.Unresolved, state.keyIdentity)
    assertEquals(ServerSettings("", ""), store.current)
  }

  @Test
  fun `importar configuracao limpa estado de sync antes de sincronizar`() = runTest(dispatcher) {
    val store = FakeSettingsStore(ServerSettings("", ""))
    val manager = syncManager(backgroundScope)
    var resets = 0
    var lastResultAtReset: String? = null
    var statusAtReset: SyncStatus? = null
    val viewModel =
      ServerConfigViewModel(
        store,
        manager,
        fetchKeyIdentity = { null },
        resetSyncState = {
          resets++
          lastResultAtReset = manager.lastResult.value
          statusAtReset = manager.status.value
        },
      )
    viewModel.importKey(
      """{"server_url":"https://s","private_seed":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","user_name":"carlos"}""",
    )
    advanceUntilIdle()
    assertEquals(1, resets)
    // The reset ran before the sync, so nothing from the previous server had
    // been recorded yet when it executed and the sync did happen afterwards.
    assertNull(lastResultAtReset)
    assertEquals(SyncStatus.IDLE, statusAtReset)
    // The sync was triggered after the reset (the status left IDLE).
    assertTrue(manager.status.value != SyncStatus.IDLE)
  }

  @Test
  fun `clearConfig limpa estado de sync sem apagar registos locais`() = runTest(dispatcher) {
    val store = FakeSettingsStore(ServerSettings("https://s", "seed"))
    val records = mutableListOf("1", "2")
    var watermark: Long? = 1234L
    var photoHashes = 5
    var clockOffset: Long? = 3600L
    val viewModel =
      ServerConfigViewModel(
        store,
        syncManager(backgroundScope),
        fetchKeyIdentity = { KeyIdentityDto("k1", "maria") },
        resetSyncState = {
          watermark = null
          photoHashes = 0
          clockOffset = null
        },
      )
    advanceUntilIdle()

    viewModel.clearConfig()
    advanceUntilIdle()

    assertNull(watermark)
    assertEquals(0, photoHashes)
    assertNull(clockOffset)
    assertEquals(listOf("1", "2"), records)
  }
}

private class FakeSettingsStore(var current: ServerSettings) : SettingsStore {
  override fun settings(): ServerSettings = current

  override fun save(baseUrl: String, signingKey: String, userName: String) {
    current = ServerSettings(baseUrl, signingKey, userName)
  }
}
