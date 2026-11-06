package com.example.elisdb

import android.app.Application
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.sync.PhotoStoreGuard
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.maplibre.android.MapLibre

class ElisdbApp : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    MapLibre.getInstance(this)
    container = AppContainer(this)
    // Background transfers are opt-in. The launch sync still covers the first
    // pull/push of the session; saving a client and the manual action trigger
    // their own runs. A transient failure schedules a one-off retry with
    // exponential backoff even when periodic sync is disabled.
    container.periodicSyncScheduler.scheduleIfEnabled()
    container.applicationScope.launch {
      initialSyncIfConfigured(
        settingsStore = container.settingsStore,
        syncNow = { container.syncManager.syncNow() },
      )
    }
    container.applicationScope.launch {
      container.syncManager.fault.collect { fault ->
        if (fault == SyncFault.NETWORK) container.periodicSyncScheduler.scheduleRetry()
      }
    }
    container.applicationScope.launch {
      container.syncManager.status.collect { status ->
        if (status == SyncStatus.SUCCESS) container.periodicSyncScheduler.cancelRetry()
      }
    }
    container.applicationScope.launch {
      PhotoStoreGuard.mutex.withLock { container.photoSweeper.sweep() }
    }
    // Measure the clock offset at startup too: edits made before the first sync
    // then carry a timestamp the server accepts (a wrong device clock used to
    // stamp records so far in the past that other devices never pulled them).
    container.applicationScope.launch { container.refreshClock() }
  }
}

/**
 * Runs the initial sync of the session, but only when a server is configured:
 * without a signing key the sync could only fail with a configuration fault,
 * which would greet offline users with an error they cannot act on. The sync
 * runs in the application scope and is single-flight, so saving a client right
 * after launch joins this run instead of starting a second one.
 */
internal suspend fun initialSyncIfConfigured(
  settingsStore: SettingsStore,
  syncNow: suspend () -> Unit,
) {
  if (settingsStore.settings().signingKey.isNotBlank()) syncNow()
}
