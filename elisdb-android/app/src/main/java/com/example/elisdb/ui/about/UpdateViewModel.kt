package com.example.elisdb.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.BuildConfig
import com.example.elisdb.remote.AppReleaseDto
import com.example.elisdb.remote.ReleaseVerificationException
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which step of the check/download flow the update card is showing. */
sealed interface UpdatePhase {
  data object Idle : UpdatePhase

  data object Checking : UpdatePhase

  /** The installed build is the newest one published. */
  data object UpToDate : UpdatePhase

  /** The server publishes no build at all. */
  data object NoRelease : UpdatePhase

  /** No server (or signing key) is configured, so there is nothing to ask. */
  data object NotConfigured : UpdatePhase

  data class Available(val release: AppReleaseDto) : UpdatePhase

  data class Downloading(val release: AppReleaseDto, val progress: Float?) : UpdatePhase

  data class Ready(val release: AppReleaseDto, val file: File) : UpdatePhase

  /** [release] is kept when the download failed so "retry" repeats just that step. */
  data class Failed(val kind: FailureKind, val release: AppReleaseDto? = null) : UpdatePhase
}

enum class FailureKind { CHECK, DOWNLOAD, CHECKSUM }

/**
 * Drives the "Atualizações" card on the About screen: asks the server for the
 * published build, compares it with the installed `versionCode`, downloads the
 * package with progress and hands it over once its checksum matches.
 *
 * The network work is injected so the flow can be tested without a server.
 */
class UpdateViewModel(
  private val installedVersionCode: Int = BuildConfig.VERSION_CODE,
  private val isConfigured: () -> Boolean = { true },
  private val fetchRelease: suspend () -> AppReleaseDto? = { null },
  private val downloadRelease: suspend (AppReleaseDto, (Long, Long?) -> Unit) -> File =
    { _, _ -> throw IllegalStateException("no downloader configured") },
) : ViewModel() {
  private val _phase = MutableStateFlow<UpdatePhase>(UpdatePhase.Idle)
  val phase: StateFlow<UpdatePhase> = _phase.asStateFlow()

  private var job: Job? = null

  /** Asks the server which build it publishes and compares it with this one. */
  fun check() {
    if (job?.isActive == true) return
    if (!isConfigured()) {
      _phase.value = UpdatePhase.NotConfigured
      return
    }
    job = viewModelScope.launch {
      _phase.value = UpdatePhase.Checking
      _phase.value =
        try {
          val release = fetchRelease()
          when {
            release == null -> UpdatePhase.NoRelease
            release.versionCode > installedVersionCode -> UpdatePhase.Available(release)
            else -> UpdatePhase.UpToDate
          }
        } catch (e: Exception) {
          UpdatePhase.Failed(FailureKind.CHECK)
        }
    }
  }

  /** Downloads the offered build; the card only enables "Instalar" once verified. */
  fun download() {
    val release = (_phase.value as? UpdatePhase.Available)?.release ?: return
    download(release)
  }

  /** Repeats the failed step: the download when there was one, else the check. */
  fun retry() {
    val failed = _phase.value as? UpdatePhase.Failed ?: return
    val release = failed.release
    if (release != null) download(release) else check()
  }

  fun reset() {
    if (job?.isActive != true) _phase.value = UpdatePhase.Idle
  }

  private fun download(release: AppReleaseDto) {
    if (job?.isActive == true) return
    job = viewModelScope.launch {
      _phase.value = UpdatePhase.Downloading(release, null)
      _phase.value =
        try {
          val file =
            downloadRelease(release) { received, total ->
              val progress =
                if (total != null && total > 0) {
                  (received.toFloat() / total).coerceIn(0f, 1f)
                } else {
                  null
                }
              _phase.value = UpdatePhase.Downloading(release, progress)
            }
          UpdatePhase.Ready(release, file)
        } catch (e: ReleaseVerificationException) {
          UpdatePhase.Failed(FailureKind.CHECKSUM, release)
        } catch (e: Exception) {
          UpdatePhase.Failed(FailureKind.DOWNLOAD, release)
        }
    }
  }
}
