package com.example.elisdb

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.sync.PhotoStoreGuard
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.osmdroid.config.Configuration

class ElisdbApp : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    Configuration.getInstance().userAgentValue = osmdroidUserAgent()
    container = AppContainer(this)
    // No periodic sync: transfers are triggered by the app start, by saving a
    // client and by the manual action. The Sync screen checks pending work on
    // open; this launch only covers the first pull/push of the session.
    container.applicationScope.launch {
      initialSyncIfConfigured(
        settingsStore = container.settingsStore,
        syncNow = { container.syncManager.syncNow() },
      )
    }
    container.applicationScope.launch {
      PhotoStoreGuard.mutex.withLock { container.photoSweeper.sweep() }
    }
    // Measure the clock offset at startup too: edits made before the first sync
    // then carry a timestamp the server accepts (a wrong device clock used to
    // stamp records so far in the past that other devices never pulled them).
    container.applicationScope.launch { container.refreshClock() }
  }

  private fun osmdroidUserAgent(): String {
    val versionName =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
      } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionName
      }
    return if (versionName.isNullOrBlank()) "ElisDB (Android)" else "ElisDB/$versionName (Android)"
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
