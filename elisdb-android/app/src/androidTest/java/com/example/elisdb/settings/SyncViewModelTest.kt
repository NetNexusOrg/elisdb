package com.example.elisdb.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.sync.ServerInfo
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.ui.settings.SyncViewModel
import java.util.concurrent.atomic.AtomicInteger
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.map
import org.junit.Before
import org.junit.Test

class SyncViewModelTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  fun setUp() {
    context.deleteSharedPreferences("elisdb_settings")
  }

  private fun syncManager() =
    SyncManager(
      SyncCoordinatorProvider {
        throw RuntimeException("no network in sync test")
      },
    )

  private fun vm(
    manager: SyncManager,
    clienteCount: kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.flowOf(0),
  ): SyncViewModel = SyncViewModel(manager, clienteCount)

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
    val vm = vm(manager)
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
      vm(
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

  private fun waitForStatus(vm: SyncViewModel, status: SyncStatus) {
    val deadline = System.currentTimeMillis() + 5_000
    while (vm.uiState.value.syncStatus != status && System.currentTimeMillis() < deadline) {
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      Thread.sleep(50)
    }
  }

  private fun waitForSyncFinished(vm: SyncViewModel) {
    val deadline = System.currentTimeMillis() + 5_000
    while (vm.uiState.value.syncStatus == SyncStatus.IN_PROGRESS && System.currentTimeMillis() < deadline) {
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      Thread.sleep(50)
    }
  }

  private fun waitForPendingDownloads(vm: SyncViewModel, pending: Boolean) {
    val deadline = System.currentTimeMillis() + 5_000
    while (vm.uiState.value.hasPendingDownloads != pending && System.currentTimeMillis() < deadline) {
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      Thread.sleep(50)
    }
  }

  @Test
  fun pendingUploads_saoExpotasNoUiState() {
    val dao = FakeAndroidClienteDao()
    val manager = pendingSyncManager(dao = dao)
    val vm = vm(manager, dao.observeCount())
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
    val vm = vm(manager, dao.observeCount())
    Thread.sleep(200)
    assertFalse(vm.uiState.value.hasPendingUploads)
  }

  @Test
  fun pendingDownloads_saoExpotasNoUiState() {
    val watermark = FakeAndroidPullWatermark(100L)
    val manager = pendingSyncManager(watermark = watermark, info = { ServerInfo(500L, 0) })
    val vm = vm(manager)
    vm.refreshPendingState()
    Thread.sleep(200)
    assertTrue(vm.uiState.value.hasPendingDownloads)
  }

  @Test
  fun serverClienteCount_eExpotaNoUiState() {
    val manager = pendingSyncManager(info = { ServerInfo(100L, 42) })
    val vm = vm(manager)
    vm.refreshPendingState()
    Thread.sleep(200)
    assertEquals(42, vm.uiState.value.serverClienteCount)
  }

  @Test
  fun totalClientes_contaExistentesExcluindoApagadosPendentes() {
    val dao = FakeAndroidClienteDao()
    val manager = pendingSyncManager(dao = dao)
    val vm = vm(manager, dao.observeCount())
    dao.add(Cliente(id = "1", nome = "A", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0))
    dao.add(Cliente(id = "2", nome = "B", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0))
    dao.add(Cliente(id = "3", nome = "C", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0, deletePending = true))
    Thread.sleep(200)
    assertEquals(2, vm.uiState.value.totalClientes)
  }

  @Test
  fun pendingUploadCount_eExpotaNoUiState() {
    val dao = FakeAndroidClienteDao()
    val manager = pendingSyncManager(dao = dao)
    val vm = vm(manager, dao.observeCount())
    dao.add(Cliente(id = "1", nome = "A", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0, pending = true))
    dao.add(Cliente(id = "2", nome = "B", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0))
    Thread.sleep(200)
    assertEquals(1, vm.uiState.value.pendingUploadCount)
  }

  @Test
  fun lastSuccessAt_lidoDoStore() {
    val lastSync = FakeAndroidLastSyncStore(1234L)
    val manager =
      SyncManager(
        SyncCoordinatorProvider { throw RuntimeException("no network in sync test") },
        lastSyncStore = lastSync,
      )
    val vm = vm(manager)
    Thread.sleep(200)
    assertEquals(1234L, vm.uiState.value.lastSuccessAt)
  }

  @Test
  fun verificacaoAoAbrir_atualizaPendentesSemTransferir() {
    // No separate check action: opening the screen refreshes the pending state.
    val manager =
      SyncManager(
        SyncCoordinatorProvider { throw RuntimeException("no network in sync test") },
        pullWatermark = FakeAndroidPullWatermark(null),
        fetchServerInfo = { ServerInfo(500L, 12) },
      )
    val vm = vm(manager)
    waitForPendingDownloads(vm, true)
    assertEquals(true, vm.uiState.value.hasPendingDownloads)
    assertEquals(12, vm.uiState.value.serverClienteCount)
  }

  @Test
  fun clockSkew_eExpotaNoUiState() {
    val skew = kotlinx.coroutines.flow.MutableStateFlow<Long?>(-300L)
    val vm =
      SyncViewModel(
        syncManager(),
        kotlinx.coroutines.flow.flowOf(0),
        clockSkewSeconds = skew,
      )
    Thread.sleep(200)
    assertEquals(-300L, vm.uiState.value.clockSkewSeconds)
  }

  @Test
  fun corrigirRelogio_pedeNovaMedicao() {
    val calls = AtomicInteger()
    val vm =
      SyncViewModel(
        syncManager(),
        kotlinx.coroutines.flow.flowOf(0),
        onCorrectClock = { calls.incrementAndGet() },
      )
    vm.correctClock()
    Thread.sleep(300)
    assertEquals(1, calls.get())
    assertFalse(vm.uiState.value.clockCorrecting)
  }

  private fun pendingSyncManager(
    dao: FakeAndroidClienteDao = FakeAndroidClienteDao(),
    watermark: FakeAndroidPullWatermark = FakeAndroidPullWatermark(null),
    info: () -> ServerInfo? = { null },
  ) =
    SyncManager(
      SyncCoordinatorProvider {
        throw RuntimeException("no network in sync test")
      },
      clienteDao = dao,
      pullWatermark = watermark,
      fetchServerInfo = info,
    )
}

private class FakeAndroidClienteDao : com.example.elisdb.data.local.ClienteDao {
  private val clients = kotlinx.coroutines.flow.MutableStateFlow<List<Cliente>>(emptyList())
  fun add(c: Cliente) {
    clients.value = clients.value + c
  }

  override fun observeAll(): kotlinx.coroutines.flow.Flow<List<Cliente>> = clients

  override fun observeAllIncludingDeleted(): kotlinx.coroutines.flow.Flow<List<Cliente>> = clients

  override fun observeCount(): kotlinx.coroutines.flow.Flow<Int> =
    clients.map { list -> list.count { !it.deletePending } }

  override fun observeLocalOnlyCount(): kotlinx.coroutines.flow.Flow<Int> =
    clients.map { list -> list.count { it.localOnly && !it.deletePending } }

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
  override suspend fun markSynced(id: String, seenRevision: Long): Int = 1
  override suspend fun delete(cliente: Cliente) = Unit
}

private class FakeAndroidPullWatermark(var value: Long?) : com.example.elisdb.sync.PullWatermark {
  override suspend fun get(): Long? = value
  override suspend fun set(value: Long) {
    this.value = value
  }

  override suspend fun clear() {
    value = null
  }
}

private class FakeAndroidLastSyncStore(
  var value: Long? = null,
) : com.example.elisdb.sync.LastSyncStore {
  override fun getLastSuccessAt(): Long? = value

  override fun setLastSuccessAt(epochSeconds: Long) {
    value = epochSeconds
  }

  override fun clear() {
    value = null
  }
}
