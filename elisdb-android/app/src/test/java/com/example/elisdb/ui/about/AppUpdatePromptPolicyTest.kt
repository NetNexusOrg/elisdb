package com.example.elisdb.ui.about

import com.example.elisdb.remote.AppReleaseDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatePromptPolicyTest {

  private fun release(code: Int) =
    AppReleaseDto(
      versionName = "1.13.$code",
      versionCode = code,
      size = 18_000_000,
      sha256 = "abc",
      filename = "elisdb-1.13.$code-$code.apk",
    )

  @Test
  fun novaVersaoNaoAdiadaMostraAvido() {
    assertTrue(shouldShowUpdatePrompt(UpdatePhase.Available(release(20)), 0))
  }

  @Test
  fun versaoAdiadaNaoMostraAvido() {
    assertFalse(shouldShowUpdatePrompt(UpdatePhase.Available(release(20)), 20))
  }

  @Test
  fun versaoAindaMaisNovaVoltaAMostrarAvido() {
    assertTrue(shouldShowUpdatePrompt(UpdatePhase.Available(release(21)), 20))
  }

  @Test
  fun fasesSemVersaoNovaNaoMostramAviso() {
    val phases =
      listOf(
        UpdatePhase.Idle,
        UpdatePhase.Checking,
        UpdatePhase.UpToDate,
        UpdatePhase.NoRelease,
        UpdatePhase.NotConfigured,
        UpdatePhase.Offline,
        UpdatePhase.Failed(FailureKind.CHECK),
      )

    phases.forEach { phase ->
      assertFalse(phase.toString(), shouldShowUpdatePrompt(phase, 0))
    }
  }
}
