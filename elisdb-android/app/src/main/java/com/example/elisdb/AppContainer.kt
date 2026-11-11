package com.example.elisdb

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.withTransaction
import com.example.elisdb.data.ClienteRepositoryImpl
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_1_2
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_2_3
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_3_4
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_4_5
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_5_6
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_6_7
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_7_8
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_8_9
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_9_10
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_10_11
import com.example.elisdb.data.local.AppDatabase.Companion.MIGRATION_11_12
import com.example.elisdb.data.local.RoomPullWatermark
import com.example.elisdb.location.LocationService
import com.example.elisdb.remote.ApiClient
import com.example.elisdb.remote.ApiClientProvider
import com.example.elisdb.remote.ClientHolder
import com.example.elisdb.settings.PrefsSettingsStore
import com.example.elisdb.settings.ServerSettings
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.settings.hasCredentials
import com.example.elisdb.ui.cliente.ClientViewPreferences
import com.example.elisdb.ui.cliente.PrefsClientViewPreferences
import com.example.elisdb.sync.ClockOffsetStore
import com.example.elisdb.sync.ClockSync
import com.example.elisdb.sync.FilePhotoStore
import com.example.elisdb.sync.LastSyncStore
import com.example.elisdb.sync.PhotoPushState
import com.example.elisdb.sync.PhotoStoreGuard
import com.example.elisdb.sync.PhotoSweeper
import com.example.elisdb.sync.PrefsClockOffsetStore
import com.example.elisdb.sync.PrefsLastSyncStore
import com.example.elisdb.sync.LocalSnapshotStore
import com.example.elisdb.sync.PrefsPhotoPushState
import com.example.elisdb.sync.PrefsServerIdentityStore
import com.example.elisdb.sync.PrefsStagedPhotoRegistry
import com.example.elisdb.sync.ServerAvailabilityMonitor
import com.example.elisdb.sync.ServerIdentityStore
import com.example.elisdb.sync.StagedPhotoRegistry
import com.example.elisdb.sync.SyncCoordinator
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncRetryScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.withLock

class AppContainer(context: Context) {
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  val database: AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "elisdb.db")
      .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
      .build()

  val locationService: LocationService = LocationService(context)

  val settingsStore: SettingsStore = PrefsSettingsStore(context)

  /** Latest saved configuration, so the UI reacts when a credential is imported or removed. */
  private val _settings = MutableStateFlow(settingsStore.settings())
  val settings: StateFlow<ServerSettings> = _settings.asStateFlow()

  /** Re-reads the configuration after a save or clear performed by the settings screen. */
  fun refreshSettings() {
    _settings.value = settingsStore.settings()
  }

  /** Schedules the one-off retry after a transient network failure. */
  val syncRetryScheduler: SyncRetryScheduler =
    SyncRetryScheduler(
      context,
      isConfigured = {
        settingsStore.settings().hasCredentials()
      },
    )

  val clientViewPreferences: ClientViewPreferences = PrefsClientViewPreferences(context)

  val photoStore: FilePhotoStore = FilePhotoStore(context)

  /** Photos staged in the form but not saved yet, protected from the sweeper. */
  val stagedPhotoRegistry: StagedPhotoRegistry = PrefsStagedPhotoRegistry(context)

  /** Reclaims files no photo row references; run post-pull and on app start. */
  val photoSweeper: PhotoSweeper =
    PhotoSweeper(database.fotoDao(), photoStore, stagedPhotoRegistry)

  /** High-water mark of server `updated_at` seen on pulls, used for incremental pulls (R7). */
  val watermark = RoomPullWatermark(database.syncMetaDao())

  /** Device↔server clock offset, refreshed on each successful sync. */
  val clockOffsetStore: ClockOffsetStore = PrefsClockOffsetStore(context)

  /**
   * Keeps signed timestamps on the server's timescale and exposes the measured
   * skew so the settings screen can warn about a wrong device clock.
   */
  val clockSync: ClockSync = ClockSync(clockOffsetStore)

  /** Timestamp of the last successful sync, shown on the synchronization screen. */
  val lastSyncStore: LastSyncStore = PrefsLastSyncStore(context)

  /**
   * Device clock (epoch seconds) shifted by the last-known server offset, so
   * signed requests carry a timestamp the server accepts even when the device
   * clock is skewed. Mirrors `SyncCoordinator.normalizedEpochSeconds()`.
   */
  fun normalizedEpochSeconds(): Long = clockSync.nowSeconds()

  /**
   * Re-measures the device↔server clock offset from `/api/time`. Best-effort:
   * an unconfigured app or an offline device simply keeps the previous offset.
   */
  suspend fun refreshClock() {
    runCatching {
      withApiClient { api -> clockSync.observeServerTime(api.fetchServerTime()) }
    }
  }

  /**
   * Completes a legacy credential that carries the server and key but no user
   * name, by asking the server which user owns the key. Best-effort: offline or
   * unconfigured leaves the settings untouched, and the setup screen then asks
   * for the credential file again.
   */
  suspend fun recoverUserName() {
    val current = settingsStore.settings()
    if (!current.hasCredentials() || current.userName.isNotBlank()) return
    val identity =
      runCatching {
        withApiClient { api -> api.fetchKeyIdentity(timestamp = normalizedEpochSeconds()) }
      }.getOrNull()
    val userName = identity?.username?.trim().orEmpty()
    if (userName.isEmpty()) return
    runCatching {
      settingsStore.save(current.baseUrl, current.signingKey, userName)
      refreshSettings()
    }
  }

  /** Photo content hashes at last successful push, for incremental photo push. */
  val photoPushState: PhotoPushState = PrefsPhotoPushState(context)

  /** Identity of the server database this app is configured against. */
  val serverIdentityStore: ServerIdentityStore = PrefsServerIdentityStore(context)

  /** Recoverable local snapshot used before an explicit destructive replacement. */
  val snapshotStore: LocalSnapshotStore = LocalSnapshotStore(context)

  private val apiClients =
    ClientHolder(
      currentSignature = {
        val settings = settingsStore.settings()
        "${settings.baseUrl}|${settings.signingKey}"
      },
      factory = { ApiClientProvider.create(settingsStore, clockSync) },
      close = { it.close() },
    )

  /**
   * Runs [block] with the API client for the current settings. The lease keeps
   * the client open until the request finishes, even if the settings change
   * concurrently; a replaced client closes only after its last request.
   */
  suspend fun <T> withApiClient(block: suspend (ApiClient) -> T): T {
    val lease = apiClients.acquire()
    return try {
      block(lease.client)
    } finally {
      lease.release()
    }
  }

  /**
   * Shared reachability state for Configuração and Sobre. It probes with a
   * short timeout so an unreachable server is reported as offline instead of
   * looking like an invalid configuration.
   */
  val serverAvailability: ServerAvailabilityMonitor =
    ServerAvailabilityMonitor(
      context = context,
      settingsStore = settingsStore,
      scope = applicationScope,
      probe = { withApiClient { api -> api.fetchServerTime() } },
    )

  /** Builds a coordinator bound to the current settings and local stores. */
  private fun buildSyncCoordinator(): SyncCoordinator {
    val lease = apiClients.acquire()
    return try {
      buildSyncCoordinator(lease.client, onFinished = { lease.release() })
    } catch (e: Exception) {
      lease.release()
      throw e
    }
  }

  private fun buildSyncCoordinator(api: ApiClient, onFinished: () -> Unit): SyncCoordinator =
    SyncCoordinator(
      api = api,
      clienteDao = database.clienteDao(),
      fotoDao = database.fotoDao(),
      photoStore = photoStore,
      transaction = { block -> database.withTransaction { block() } },
      pullWatermark = watermark,
      photoSweeper = photoSweeper,
      clockOffsetStore = clockOffsetStore,
      photoPushState = photoPushState,
      serverIdentityStore = serverIdentityStore,
      onWarning = { msg -> Log.w("ElisdbSync", msg) },
      onError = { e -> Log.w("ElisdbSync", "sync run failed", e) },
      onFinished = onFinished,
    )

  val syncManager: SyncManager =
    SyncManager(
      SyncCoordinatorProvider { buildSyncCoordinator() },
      clienteDao = database.clienteDao(),
      pullWatermark = watermark,
      fetchServerInfo = {
        try {
          withApiClient { api -> api.fetchServerInfo(timestamp = normalizedEpochSeconds()) }
        } catch (e: Exception) {
          null
        }
      },
      lastSyncStore = lastSyncStore,
      replaceLocal = {
        buildSyncCoordinator().replaceLocalWithServer {
          // Keep the snapshot consistent with the photo sweep/materialization
          // that may be running on the app-start path.
          PhotoStoreGuard.mutex.withLock { snapshotStore.create() }
        }
      },
      onOutcome = serverAvailability::onSyncOutcome,
    )

  val repository: ClienteRepositoryImpl =
    ClienteRepositoryImpl(
      context,
      database,
      database.clienteDao(),
      database.fotoDao(),
      syncManager,
      clockOffsetStore,
      stagedPhotoRegistry,
    )

  /**
   * Drops everything tied to the previous server before the app starts using a
   * new one: the pull cursor (its timestamps are meaningless elsewhere), the
   * photo content hashes recorded as uploaded, the measured clock offset and
   * the last sync display. Local records are kept on purpose — the full pull
   * that follows reconciles them.
   */
  suspend fun resetSyncState() {
    // Stop and await the old run before clearing persisted state, so it cannot
    // write a stale watermark/hash after the reset.
    syncManager.resetState()
    watermark.clear()
    photoPushState.clearAll()
    clockOffsetStore.clear()
    lastSyncStore.clear()
    serverIdentityStore.clear()
    // Re-check against the new server (or show NotConfigured after removing it).
    serverAvailability.invalidate()
    // Drop any retry or legacy periodic work tied to the previous server.
    syncRetryScheduler.cancelPending()
  }
}