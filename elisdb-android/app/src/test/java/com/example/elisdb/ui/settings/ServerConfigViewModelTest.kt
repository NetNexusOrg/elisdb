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
}

private class FakeSettingsStore(var current: ServerSettings) : SettingsStore {
  override fun settings(): ServerSettings = current

  override fun save(baseUrl: String, signingKey: String) {
    current = ServerSettings(baseUrl, signingKey)
  }
}
