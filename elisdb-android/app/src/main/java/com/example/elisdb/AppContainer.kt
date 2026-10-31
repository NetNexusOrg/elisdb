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
import com.example.elisdb.data.local.RoomPullWatermark
import com.example.elisdb.location.LocationService
import com.example.elisdb.remote.ApiClient
import com.example.elisdb.remote.ApiClientProvider
import com.example.elisdb.settings.PrefsSettingsStore
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.sync.ClockOffsetStore
import com.example.elisdb.sync.ClockSync
import com.example.elisdb.sync.FilePhotoStore
import com.example.elisdb.sync.LastSyncStore
import com.example.elisdb.sync.PhotoPushState
import com.example.elisdb.sync.PhotoSweeper
import com.example.elisdb.sync.PrefsClockOffsetStore
import com.example.elisdb.sync.PrefsLastSyncStore
import com.example.elisdb.sync.PrefsPhotoPushState
import com.example.elisdb.sync.SyncCoordinator
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  val database: AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "elisdb.db")
      .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
      .build()

  val locationService: LocationService = LocationService(context)

  val settingsStore: SettingsStore = PrefsSettingsStore(context)

  val photoStore: FilePhotoStore = FilePhotoStore(context)

  /** Reclaims files no photo row references; run post-pull and on app start. */
  val photoSweeper: PhotoSweeper = PhotoSweeper(database.fotoDao(), photoStore)

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
    val api = runCatching { createApiClient() }.getOrNull() ?: return
    runCatching { clockSync.observeServerTime(api.fetchServerTime()) }
  }

  /** Photo content hashes at last successful push, for incremental photo push. */
  val photoPushState: PhotoPushState = PrefsPhotoPushState(context)

  /** Returns the API client built from the current runtime settings. */
  fun createApiClient(): ApiClient = apiClient

  /**
   * A single long-lived [ApiClient] (and its underlying HTTP connection pool)
   * shared by every request for the process lifetime, so syncs and history
   * fetches do not leak a new client per call. The client is rebuilt only when
   * the effective server settings (base URL or signing key) change; the
   * previous client is closed first.
   */
  val apiClient: ApiClient
    get() {
      val settings = settingsStore.settings()
      val signature = "${settings.baseUrl}|${settings.signingKey}"
      if (cachedApiClient == null || cachedApiSignature != signature) {
        cachedApiClient?.close()
        cachedApiClient = ApiClientProvider.create(settingsStore, clockSync)
        cachedApiSignature = signature
      }
      return cachedApiClient!!
    }

  private var cachedApiClient: ApiClient? = null
  private var cachedApiSignature: String? = null

  val syncManager: SyncManager =
    SyncManager(
      SyncCoordinatorProvider {
        SyncCoordinator(
          api = createApiClient(),
          clienteDao = database.clienteDao(),
          fotoDao = database.fotoDao(),
          photoStore = photoStore,
          transaction = { block -> database.withTransaction { block() } },
          pullWatermark = watermark,
          photoSweeper = photoSweeper,
          clockOffsetStore = clockOffsetStore,
          photoPushState = photoPushState,
          onWarning = { msg -> Log.w("ElisdbSync", msg) },
          onError = { e -> Log.w("ElisdbSync", "sync run failed", e) },
        )
      },
      clienteDao = database.clienteDao(),
      pullWatermark = watermark,
      fetchServerInfo = {
        try {
          createApiClient().fetchServerInfo(timestamp = normalizedEpochSeconds())
        } catch (e: Exception) {
          null
        }
      },
      lastSyncStore = lastSyncStore,
    )

  val repository: ClienteRepositoryImpl =
    ClienteRepositoryImpl(
      context,
      database,
      database.clienteDao(),
      database.fotoDao(),
      syncManager,
      clockOffsetStore,
    )

  /**
   * Drops everything tied to the previous server before the app starts using a
   * new one: the pull cursor (its timestamps are meaningless elsewhere), the
   * photo content hashes recorded as uploaded, the measured clock offset and
   * the last sync display. Local records are kept on purpose — the full pull
   * that follows reconciles them.
   */
  suspend fun resetSyncState() {
    watermark.clear()
    photoPushState.clearAll()
    clockOffsetStore.clear()
    lastSyncStore.clear()
    syncManager.resetState()
  }
}