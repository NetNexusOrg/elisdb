package com.example.elisdb.ui.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.elisdb.AppContainer
import com.example.elisdb.BuildConfig
import java.io.File

/**
 * Builds the [UpdateViewModel] wired to the shared container, so the About
 * screen and the launch prompt use the same check/download configuration.
 */
@Composable
fun rememberUpdateViewModel(container: AppContainer): UpdateViewModel {
  val context = LocalContext.current
  return viewModel {
    UpdateViewModel(
      installedVersionCode = BuildConfig.VERSION_CODE,
      isConfigured = {
        val settings = container.settingsStore.settings()
        settings.baseUrl.isNotBlank() && settings.signingKey.isNotBlank()
      },
      serverAvailability = container.serverAvailability.state,
      refreshServerAvailability = { container.serverAvailability.refresh() },
      fetchRelease = {
        container.withApiClient { api ->
          api.fetchLatestRelease(container.normalizedEpochSeconds())
        }
      },
      downloadRelease = { release, onProgress ->
        container.withApiClient { api ->
          api.downloadRelease(
            release = release,
            targetDir = File(context.cacheDir, "updates"),
            timestamp = container.normalizedEpochSeconds(),
            onProgress = onProgress,
          )
        }
      },
    )
  }
}
