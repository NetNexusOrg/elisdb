package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.sync.SyncManager.Companion.mapOutcome
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncManagerTest {

  @Test
  fun `stale clock maps to CLOCK fault and not AUTH`() {
    val display = mapOutcome(SyncOutcome.ClockFailure)
    assertEquals(SyncStatus.ERROR, display.status)
    assertEquals(SyncFault.CLOCK, display.fault)
  }

  @Test
  fun `invalid key still maps to AUTH fault`() {
    val display = mapOutcome(SyncOutcome.AuthFailure)
    assertEquals(SyncStatus.ERROR, display.status)
    assertEquals(SyncFault.AUTH, display.fault)
  }

  @Test
  fun `success maps to SUCCESS with no fault`() {
    val display = mapOutcome(SyncOutcome.Success)
    assertEquals(SyncStatus.SUCCESS, display.status)
    assertEquals(null, display.fault)
  }

  @Test
  fun `success with stale records surfaces a lost-edit message`() {
    val display = mapOutcome(SyncOutcome.SuccessWithStatuses(accepted = 0, stale = listOf("Maria")))
    assertEquals(SyncStatus.SUCCESS, display.status)
    assertEquals(null, display.fault)
    val message = display.lastResult
    assertTrue("lost edit surfaced", message != null && message.contains("Maria"))
  }

  @Test
  fun `success with empty stale is a plain success`() {
    val display = mapOutcome(SyncOutcome.SuccessWithStatuses(accepted = 1, stale = emptyList()))
    assertEquals(SyncStatus.SUCCESS, display.status)
    assertEquals(null, display.lastResult)
  }

  @Test
  fun `transient error maps to NETWORK fault`() {
    assertEquals(SyncFault.NETWORK, mapOutcome(SyncOutcome.TransientError).fault)
  }

  @Test
  fun `config fault maps to CONFIG fault`() {
    assertEquals(SyncFault.CONFIG, mapOutcome(SyncOutcome.ConfigFault).fault)
  }

  @Test
  fun `single flight concurrent syncs share one coordinator run`() = runBlocking {
    val releasePull = CompletableDeferred<Unit>()
    val api = FakeSyncApi().apply { pullGate = releasePull }
    var coordinatorCalls = 0
    val manager =
      SyncManager(
        SyncCoordinatorProvider {
          coordinatorCalls++
          SyncCoordinator(api, FakeClienteDao(), FakeFotoDao(), FakePhotoStore())
        },
      )

    val first = launch { manager.sync() }
    while (api.pulledSinces.isEmpty()) delay(5)
    val second = launch { manager.sync() }
    delay(50)
    releasePull.complete(Unit)
    first.join()
    second.join()

    assertEquals("only one coordinator run starts", 1, coordinatorCalls)
    assertEquals("only one pull happens", 1, api.pulledSinces.size)
  }

  @Test
  fun `transient failure updates status and fault`() = runBlocking {
    val api = FakeSyncApi().apply { failWith = IOException("boom") }
    val manager =
      SyncManager(
        SyncCoordinatorProvider {
          SyncCoordinator(api, FakeClienteDao(), FakeFotoDao(), FakePhotoStore())
        },
      )

    val outcome = manager.sync()

    assertEquals(SyncOutcome.TransientError, outcome)
    assertEquals(SyncStatus.ERROR, manager.status.value)
    assertEquals(SyncFault.NETWORK, manager.fault.value)
  }

  @Test
  fun `manual sync joins an in-flight run producing exactly one push`() = runBlocking {
    val releasePull = CompletableDeferred<Unit>()
    val api = FakeSyncApi().apply { pullGate = releasePull }
    val pending = Cliente(id = "1", nome = "A", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0, pending = true)
    var coordinatorCalls = 0
    val manager =
      SyncManager(
        SyncCoordinatorProvider {
          coordinatorCalls++
          SyncCoordinator(api, FakeClienteDao(listOf(pending)), FakeFotoDao(), FakePhotoStore())
        },
      )

    val first = launch { manager.sync() } // run already in flight
    while (api.pushCalls == 0) delay(5)
    val manual = launch { manager.syncNow() }
    delay(50)
    releasePull.complete(Unit)
    first.join()
    manual.join()

    assertEquals("exactly one push", 1, api.pushCalls)
    assertEquals("exactly one pull", 1, api.pulledSinces.size)
    assertEquals("manual did not start a second run", 1, coordinatorCalls)
    assertTrue("status reflects the joined run", manager.status.value == SyncStatus.SUCCESS)
  }

  @Test
  fun `pending uploads reflected when dao has pending records`() = runBlocking {
    val pending = Cliente(id = "1", nome = "A", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0, pending = true)
    val dao = FakeClienteDao(listOf(pending))
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        clienteDao = dao,
      )
    // observeAll is a cold flow; give the collector a tick.
    delay(50)
    assertTrue(manager.hasPendingUploads.value)
  }

  @Test
  fun `no pending uploads when dao has none`() = runBlocking {
    val dao = FakeClienteDao()
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        clienteDao = dao,
      )
    delay(50)
    assertTrue(!manager.hasPendingUploads.value)
  }

  @Test
  fun `pending downloads set when latest exceeds watermark`() = runBlocking {
    val watermark = FakePullWatermark(value = 100L)
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        pullWatermark = watermark,
        fetchServerInfo = { ServerInfo(200L, 0) },
      )
    manager.refreshPendingDownloads()
    assertTrue(manager.hasPendingDownloads.value)
  }

  @Test
  fun `pending downloads set on fresh install when server has data`() = runBlocking {
    val watermark = FakePullWatermark(value = null)
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        pullWatermark = watermark,
        fetchServerInfo = { ServerInfo(500L, 0) },
      )
    manager.refreshPendingDownloads()
    assertTrue(manager.hasPendingDownloads.value)
  }

  @Test
  fun `no pending downloads on fresh install when server is empty`() = runBlocking {
    val watermark = FakePullWatermark(value = null)
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        pullWatermark = watermark,
        fetchServerInfo = { ServerInfo(0L, 0) },
      )
    manager.refreshPendingDownloads()
    assertTrue(!manager.hasPendingDownloads.value)
  }

  @Test
  fun `no pending downloads when latest equals the watermark`() = runBlocking {
    val watermark = FakePullWatermark(value = 100L)
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        pullWatermark = watermark,
        fetchServerInfo = { ServerInfo(100L, 0) },
      )
    manager.refreshPendingDownloads()
    assertTrue(!manager.hasPendingDownloads.value)
  }

  @Test
  fun `pending downloads set when latest is below the watermark`() = runBlocking {
    // A server restored from an older backup reports a latest below the local
    // cursor; that still means there is data to re-download.
    val watermark = FakePullWatermark(value = 500L)
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        pullWatermark = watermark,
        fetchServerInfo = { ServerInfo(200L, 0) },
      )
    manager.refreshPendingDownloads()
    assertTrue(manager.hasPendingDownloads.value)
  }

  @Test
  fun `successful sync refreshes pending downloads and clears them`() = runBlocking {
    val watermark = FakePullWatermark(value = 100L)
    val api = FakeSyncApi().apply { latestUpdatedAt = 100L }
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(api, FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        pullWatermark = watermark,
        fetchServerInfo = { ServerInfo(api.latestUpdatedAt ?: 0L, api.clientCount) },
      )
    // First: server has newer data.
    api.latestUpdatedAt = 500L
    manager.refreshPendingDownloads()
    assertTrue(manager.hasPendingDownloads.value)
    // After a successful sync the pull advances the watermark past the latest.
    watermark.value = 500L
    api.latestUpdatedAt = 500L
    manager.sync()
    assertTrue(!manager.hasPendingDownloads.value)
  }

  @Test
  fun `refresh exposes the server client count`() = runBlocking {
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        pullWatermark = FakePullWatermark(value = 100L),
        fetchServerInfo = { ServerInfo(100L, 480) },
      )
    assertNull(manager.serverClienteCount.value)
    manager.refreshPendingDownloads()
    assertEquals(480, manager.serverClienteCount.value)
  }

  @Test
  fun `server client count stays unknown when the fetch fails`() = runBlocking {
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        pullWatermark = FakePullWatermark(value = 100L),
        fetchServerInfo = { null },
      )
    manager.refreshPendingDownloads()
    assertNull(manager.serverClienteCount.value)
  }

  @Test
  fun `pending upload count reflects pending and deleted records`() = runBlocking {
    val records =
      listOf(
        Cliente(id = "1", nome = "A", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0, pending = true),
        Cliente(id = "2", nome = "B", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0, deletePending = true),
        Cliente(id = "3", nome = "C", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
      )
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        clienteDao = FakeClienteDao(records),
      )
    delay(50)
    assertEquals(2, manager.pendingUploadCount.value)
  }

  @Test
  fun `successful sync records and persists last success time`() = runBlocking {
    val lastSyncStore = FakeLastSyncStore()
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi(), FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        lastSyncStore = lastSyncStore,
      )
    assertEquals(null, manager.lastSuccessAt.value)
    manager.sync()
    assertTrue("last success timestamp published", manager.lastSuccessAt.value != null)
    assertEquals("timestamp persisted", manager.lastSuccessAt.value, lastSyncStore.value)
  }

  @Test
  fun `failed sync does not record a last success time`() = runBlocking {
    val lastSyncStore = FakeLastSyncStore()
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(FakeSyncApi().apply { failWith = IOException("boom") }, FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        lastSyncStore = lastSyncStore,
      )
    manager.sync()
    assertEquals(null, manager.lastSuccessAt.value)
    assertEquals(null, lastSyncStore.value)
  }

  @Test
  fun `cancelled caller still publishes terminal status and last success`() = runBlocking {
    val releasePull = CompletableDeferred<Unit>()
    val api = FakeSyncApi().apply { pullGate = releasePull }
    val lastSyncStore = FakeLastSyncStore()
    val manager =
      SyncManager(
        SyncCoordinatorProvider { SyncCoordinator(api, FakeClienteDao(), FakeFotoDao(), FakePhotoStore()) },
        lastSyncStore = lastSyncStore,
      )

    val caller = launch { manager.sync() }
    while (api.pulledSinces.isEmpty()) delay(5)
    // The triggering caller goes away mid-run (screen closed → scope cancelled).
    caller.cancel()
    delay(20)
    assertEquals("run still in progress", SyncStatus.IN_PROGRESS, manager.status.value)

    releasePull.complete(Unit)
    // The run publishes its terminal state without depending on the caller.
    while (manager.status.value == SyncStatus.IN_PROGRESS) delay(5)
    assertEquals(SyncStatus.SUCCESS, manager.status.value)
    while (manager.lastSuccessAt.value == null) delay(5)
    assertEquals("timestamp persisted", manager.lastSuccessAt.value, lastSyncStore.value)
    assertEquals("a new sync can start afterwards", SyncStatus.SUCCESS, manager.syncNow())
  }
}