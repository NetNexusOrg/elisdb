package com.example.elisdb.ui.about

import com.example.elisdb.remote.AppReleaseDto
import com.example.elisdb.remote.ReleaseVerificationException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun release(code: Int = 11) =
    AppReleaseDto(
      versionName = "1.6.$code",
      versionCode = code,
      size = 1024,
      sha256 = "abc",
      filename = "elisdb-1.6.$code-$code.apk",
    )

  @Test
  fun versaoMaisRecenteFicaDisponivel() = runTest(dispatcher) {
    val published = release(11)
    val vm = UpdateViewModel(installedVersionCode = 10, fetchRelease = { published })

    vm.check()
    advanceUntilIdle()

    assertEquals(UpdatePhase.Available(published), vm.phase.value)
  }

  @Test
  fun versaoIgualOuAnteriorEstaAtualizada() = runTest(dispatcher) {
    val vm = UpdateViewModel(installedVersionCode = 10, fetchRelease = { release(10) })

    vm.check()
    advanceUntilIdle()

    assertEquals(UpdatePhase.UpToDate, vm.phase.value)
  }

  @Test
  fun semVersaoPublicadaNaoEUmErro() = runTest(dispatcher) {
    val vm = UpdateViewModel(installedVersionCode = 10, fetchRelease = { null })

    vm.check()
    advanceUntilIdle()

    assertEquals(UpdatePhase.NoRelease, vm.phase.value)
  }

  @Test
  fun semServidorConfiguradoNaoPedeNada() = runTest(dispatcher) {
    var asked = false
    val vm =
      UpdateViewModel(
        installedVersionCode = 10,
        isConfigured = { false },
        fetchRelease = {
          asked = true
          release(11)
        },
      )

    vm.check()
    advanceUntilIdle()

    assertEquals(UpdatePhase.NotConfigured, vm.phase.value)
    assertFalse(asked)
  }

  @Test
  fun falhaAoProcurarFicaComErroDeVerificacao() = runTest(dispatcher) {
    val vm = UpdateViewModel(installedVersionCode = 10, fetchRelease = { error("offline") })

    vm.check()
    advanceUntilIdle()

    assertEquals(UpdatePhase.Failed(FailureKind.CHECK), vm.phase.value)
  }

  @Test
  fun transferenciaVerificadaFicaProntaAInstalar() = runTest(dispatcher) {
    val published = release(11)
    val downloaded = File.createTempFile("update", ".apk")
    var progress = 0f
    val vm =
      UpdateViewModel(
        installedVersionCode = 10,
        fetchRelease = { published },
        downloadRelease = { _, onProgress ->
          onProgress(512, 1024)
          progress = 1f
          downloaded
        },
      )

    vm.check()
    advanceUntilIdle()
    vm.download()
    advanceUntilIdle()

    assertEquals(UpdatePhase.Ready(published, downloaded), vm.phase.value)
    assertEquals(1f, progress)
  }

  @Test
  fun checksumErradoApagaOfertaDeInstalacao() = runTest(dispatcher) {
    val published = release(11)
    val vm =
      UpdateViewModel(
        installedVersionCode = 10,
        fetchRelease = { published },
        downloadRelease = { _, _ -> throw ReleaseVerificationException("checksum") },
      )

    vm.check()
    advanceUntilIdle()
    vm.download()
    advanceUntilIdle()

    assertEquals(UpdatePhase.Failed(FailureKind.CHECKSUM, published), vm.phase.value)
  }

  @Test
  fun tentarNovamenteRepeteATransferenciaFalhada() = runTest(dispatcher) {
    val published = release(11)
    var downloads = 0
    val vm =
      UpdateViewModel(
        installedVersionCode = 10,
        fetchRelease = { published },
        downloadRelease = { _, _ ->
          downloads++
          if (downloads == 1) error("offline") else File.createTempFile("update", ".apk")
        },
      )

    vm.check()
    advanceUntilIdle()
    vm.download()
    advanceUntilIdle()
    assertTrue(vm.phase.value is UpdatePhase.Failed)

    vm.retry()
    advanceUntilIdle()

    assertEquals(2, downloads)
    assertTrue(vm.phase.value is UpdatePhase.Ready)
  }
}
