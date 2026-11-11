package com.example.elisdb.ui.settings

import com.example.elisdb.remote.KeyIdentityDto
import com.example.elisdb.settings.SecureStorageUnavailable
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
    onSettingsChanged: () -> Unit = {},
  ): ServerConfigViewModel =
    ServerConfigViewModel(
      store,
      syncManager(runScope),
      fetch,
      onSettingsChanged = onSettingsChanged,
    )

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
  fun `importar configuracao rejeita http publico sem guardar nem resetar`() = runTest(dispatcher) {
    val store = FakeSettingsStore(ServerSettings("https://old", "old-seed", "maria"))
    var resets = 0
    val viewModel =
      ServerConfigViewModel(
        store,
        syncManager(backgroundScope),
        fetchKeyIdentity = { null },
        resetSyncState = { resets++ },
      )
    viewModel.importKey(
      """{"server_url":"http://example.com:8000","private_seed":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","user_name":"carlos"}""",
    )
    advanceUntilIdle()

    assertEquals(ServerSettings("https://old", "old-seed", "maria"), store.current)
    assertEquals("URL inválida não pode reiniciar o sync", 0, resets)
    assertTrue("erro acionável mostrado", viewModel.uiState.value.signingKeyError?.contains("HTTP") == true)
  }

  @Test
  fun `importar configuracao aceita http privado`() = runTest(dispatcher) {
    val store = FakeSettingsStore(ServerSettings("", ""))
    val viewModel = vm(store, backgroundScope)
    viewModel.importKey(
      """{"server_url":"http://192.168.1.10:8000","private_seed":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","user_name":"carlos"}""",
    )
    advanceUntilIdle()

    assertEquals("http://192.168.1.10:8000", store.current.baseUrl)
    assertEquals("carlos", store.current.userName)
    assertNull(viewModel.uiState.value.signingKeyError)
  }

  @Test
  fun `importar configuracao rejeita esquema nao suportado sem guardar`() = runTest(dispatcher) {
    val store = FakeSettingsStore(ServerSettings("", ""))
    val viewModel = vm(store, backgroundScope)
    viewModel.importKey(
      """{"server_url":"ftp://example.com","private_seed":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","user_name":"carlos"}""",
    )
    advanceUntilIdle()

    assertEquals(ServerSettings("", ""), store.current)
    assertTrue("erro de URL mostrado", viewModel.uiState.value.signingKeyError != null)
  }

  @Test
  fun `importar configuracao falha de armazenamento seguro mantem a anterior`() = runTest(dispatcher) {
    val store =
      FakeSettingsStore(ServerSettings("https://old", "old-seed", "maria"), failSave = true)
    var resets = 0
    val viewModel =
      ServerConfigViewModel(
        store,
        syncManager(backgroundScope),
        fetchKeyIdentity = { null },
        resetSyncState = { resets++ },
      )
    viewModel.importKey(
      """{"server_url":"https://new","private_seed":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","user_name":"carlos"}""",
    )
    advanceUntilIdle()

    assertEquals(ServerSettings("https://old", "old-seed", "maria"), store.current)
    assertEquals("https://old", viewModel.uiState.value.baseUrl)
    assertEquals("old-seed", viewModel.uiState.value.signingKey)
    assertTrue(viewModel.uiState.value.hasKey)
    assertEquals("falha de escrita não pode reiniciar o sync", 0, resets)
    assertTrue(
      "erro de armazenamento acionável",
      viewModel.uiState.value.signingKeyError?.contains("segurança") == true,
    )
  }

  @Test
  fun `clearConfig falha de armazenamento seguro mantem a configuracao`() = runTest(dispatcher) {
    val store =
      FakeSettingsStore(ServerSettings("https://old", "old-seed", "maria"), failSave = true)
    var resets = 0
    val viewModel =
      ServerConfigViewModel(
        store,
        syncManager(backgroundScope),
        fetchKeyIdentity = { null },
        resetSyncState = { resets++ },
      )
    viewModel.clearConfig()
    advanceUntilIdle()

    assertEquals(ServerSettings("https://old", "old-seed", "maria"), store.current)
    assertEquals("https://old", viewModel.uiState.value.baseUrl)
    assertTrue(viewModel.uiState.value.hasKey)
    assertEquals("falha de escrita não pode reiniciar o sync", 0, resets)
    assertTrue(viewModel.uiState.value.signingKeyError?.contains("segurança") == true)
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

  @Test
  fun `importar configuracao notifica mudanca de configuracao`() = runTest(dispatcher) {
    var changes = 0
    val store = FakeSettingsStore(ServerSettings("", ""))
    val viewModel = vm(store, backgroundScope, onSettingsChanged = { changes++ })

    viewModel.importKey(
      """{"server_url":"https://s","private_seed":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","user_name":"carlos"}""",
    )
    advanceUntilIdle()

    assertEquals(1, changes)
  }

  @Test
  fun `clearConfig notifica mudanca de configuracao`() = runTest(dispatcher) {
    var changes = 0
    val store = FakeSettingsStore(ServerSettings("https://s", "seed", "maria"))
    val viewModel = vm(store, backgroundScope, onSettingsChanged = { changes++ })
    advanceUntilIdle()

    viewModel.clearConfig()
    advanceUntilIdle()

    assertEquals(1, changes)
  }

  @Test
  fun `lookup completa o nome em falta e notifica`() = runTest(dispatcher) {
    var changes = 0
    val store = FakeSettingsStore(ServerSettings("https://s", "seed"))
    val viewModel =
      vm(
        store,
        backgroundScope,
        fetch = { KeyIdentityDto("k1", "maria") },
        onSettingsChanged = { changes++ },
      )
    advanceUntilIdle()

    assertEquals("maria", store.current.userName)
    assertEquals(1, changes)
  }
}

private class FakeSettingsStore(
  var current: ServerSettings,
  private val failSave: Boolean = false,
) : SettingsStore {
  override fun settings(): ServerSettings = current

  override fun save(baseUrl: String, signingKey: String, userName: String) {
    if (failSave) throw SecureStorageUnavailable("no secure storage")
    current = ServerSettings(baseUrl, signingKey, userName)
  }
}
