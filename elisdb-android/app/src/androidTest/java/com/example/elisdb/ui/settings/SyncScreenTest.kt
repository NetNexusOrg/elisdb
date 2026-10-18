package com.example.elisdb.ui.settings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.settings.PrefsSettingsStore
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.sync.PullWatermark
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.theme.ElisdbTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class SyncScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private fun syncManager() =
    SyncManager(
      SyncCoordinatorProvider {
        throw RuntimeException("no network in sync screen test")
      },
    )

  private fun store(): SettingsStore {
    context.deleteSharedPreferences("elisdb_settings")
    return PrefsSettingsStore(context)
  }

  private fun syncManagerWithPendingDownloads() =
    SyncManager(
      SyncCoordinatorProvider { throw RuntimeException("no network in sync screen test") },
      pullWatermark =
        object : PullWatermark {
          override suspend fun get(): Long = 100L

          override suspend fun set(value: Long) = Unit
        },
      fetchLatestUpdatedAt = { 200L },
    )

  @Test
  fun sync_mostraTituloEAsDuasAcoesDistintas() {
    val store = store()
    val vm = SyncViewModel(store, syncManager(), flowOf(0))
    setContent(vm)
    composeTestRule.onNodeWithText("Sincronização").assertExists()
    // Check action (row icon) and the full-width sync action are distinct.
    composeTestRule.onNodeWithContentDescription("Verificar").assertExists()
    composeTestRule.onNodeWithText("Sincronizar").assertExists()
  }

  @Test
  fun sync_mostraUltimaSincronizacaoEDados() {
    val store = store()
    val vm = SyncViewModel(store, syncManager(), flowOf(0))
    setContent(vm)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Última sincronização").assertExists()
    composeTestRule.onNodeWithText("Nunca").assertExists()
    composeTestRule.onNodeWithText("Alterações por enviar").assertExists()
    composeTestRule.onNodeWithText("Alterações por receber").assertExists()
    // Numeric pending-downloads value, like the uploads row (no "Nada a receber"):
    // the three numeric rows (total, uploads, downloads) all read "0" here.
    composeTestRule.onNodeWithText("Nada a receber").assertDoesNotExist()
    composeTestRule.onAllNodesWithText("0").assertCountEquals(3)
  }

  @Test
  fun sync_mostraNumeroQuandoHaAlteracoesPorReceber() {
    val store = store()
    val vm = SyncViewModel(store, syncManagerWithPendingDownloads(), flowOf(0))
    setContent(vm)
    composeTestRule.waitForIdle()
    // Numeric pending-downloads value — never the old "Disponível"/"Nada a receber" labels.
    composeTestRule.onNodeWithText("Nada a receber").assertDoesNotExist()
    composeTestRule.onNodeWithText("Disponível").assertDoesNotExist()
    composeTestRule.onNodeWithText("≥1").assertExists()
  }

  @Test
  fun sync_mostraTotalDeClientes() {    val store = store()
    val vm = SyncViewModel(store, syncManager(), flowOf(3))
    setContent(vm)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Clientes guardados").assertExists()
    composeTestRule.onNodeWithText("3").assertExists()
  }

  @Test
  fun sync_semConfiguracaoMostraNaoConfigurado() {
    val store = store()
    val vm = SyncViewModel(store, syncManager(), flowOf(0))
    setContent(vm)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Não configurado").assertExists()
  }

  @Test
  fun sync_comConfiguracaoMostraSincronizado() {
    val store = store()
    store.save("https://db.example.com", "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=")
    val vm = SyncViewModel(store, syncManager(), flowOf(0))
    setContent(vm)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Sincronizado").assertExists()
  }

  private fun setContent(vm: SyncViewModel) {
    composeTestRule.setContent {
      ElisdbTheme {
        SyncScreen(viewModel = vm, onBack = {})
      }
    }
  }
}
