package com.example.elisdb.ui.about

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.elisdb.BuildConfig
import com.example.elisdb.sync.ServerAvailability
import kotlinx.coroutines.flow.StateFlow

/** Sentinel stored while no version has been postponed or opened this launch. */
internal const val NO_DISMISSED_VERSION = 0

/**
 * Tells whether the launch prompt should be visible: only when the check found
 * a newer build and that build has not been postponed (or opened) this launch.
 */
internal fun shouldShowUpdatePrompt(phase: UpdatePhase, dismissedVersionCode: Int): Boolean {
  val release = (phase as? UpdatePhase.Available)?.release ?: return false
  return release.versionCode != dismissedVersionCode
}

/**
 * Asks for a newer published build when the app starts and offers to open the
 * About screen, where the package is downloaded and installed.
 *
 * "Adiar" silences the prompt until the next launch by remembering the version
 * code for this composition only (it survives rotation, not a process restart).
 */
@Composable
fun AppUpdatePrompt(
  updateViewModel: UpdateViewModel,
  serverAvailability: StateFlow<ServerAvailability>,
  isAboutOpen: Boolean,
  onUpdate: () -> Unit,
) {
  val phase by updateViewModel.phase.collectAsState()
  val availability by serverAvailability.collectAsState()
  var dismissedVersionCode by rememberSaveable { mutableIntStateOf(NO_DISMISSED_VERSION) }

  LaunchedEffect(updateViewModel, availability, isAboutOpen) {
    if (!isAboutOpen && availability == ServerAvailability.Online) updateViewModel.check()
  }

  if (isAboutOpen || !shouldShowUpdatePrompt(phase, dismissedVersionCode)) return
  val release = (phase as UpdatePhase.Available).release

  AlertDialog(
    onDismissRequest = { dismissedVersionCode = release.versionCode },
    title = { Text("Atualização disponível") },
    text = {
      Text(
        "A versão ${release.versionName} está disponível. " +
          "Tem a versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) instalada.",
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          dismissedVersionCode = release.versionCode
          onUpdate()
        },
      ) {
        Text("Atualizar")
      }
    },
    dismissButton = {
      TextButton(onClick = { dismissedVersionCode = release.versionCode }) {
        Text("Adiar")
      }
    },
  )
}
