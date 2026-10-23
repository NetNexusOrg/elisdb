package com.example.elisdb

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
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
    // No periodic sync: synchronization is manual only. The Sync screen
    // checks pending work on open; data transfers happen only on the manual action.
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
