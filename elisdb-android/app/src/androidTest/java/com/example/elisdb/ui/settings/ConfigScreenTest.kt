package com.example.elisdb.ui.settings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.settings.PrefsSettingsStore
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.sync.PullWatermark
import com.example.elisdb.sync.ServerInfo
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.theme.ElisdbTheme
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ConfigScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private fun syncManager() =
    SyncManager(
      SyncCoordinatorProvider {
        throw RuntimeException("no network in config screen test")
      },
    )

  private fun store(): SettingsStore {
    context.deleteSharedPreferences("elisdb_settings")
    return PrefsSettingsStore(context)
  }

  private fun configure(store: SettingsStore) {
    store.save(
      "https://db.example.com",
      "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
      "maria",
    )
  }

  private fun syncManagerWithServerInfo(latest: Long, clientCount: Int) =
    SyncManager(
      SyncCoordinatorProvider { throw RuntimeException("no network in config screen test") },
      pullWatermark =
        object : PullWatermark {
          override suspend fun get(): Long = 100L

          override suspend fun set(value: Long) = Unit
        },
      fetchServerInfo = { ServerInfo(latest, clientCount) },
    )

  @Test
  fun config_mostraTituloEAcaoDeSincronizar() {
    val store = store()
    configure(store)
    setContent(store)
    composeTestRule.onNodeWithText("Configuração").assertExists()
    // A single sync action; the pending state is checked when the screen opens.
    composeTestRule.onNodeWithContentDescription("Verificar").assertDoesNotExist()
    composeTestRule.onNodeWithText("Sincronizar").assertExists()
  }

  @Test
  fun config_mostraUltimaSincronizacaoEDados() {
    val store = store()
    setContent(store)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Ainda sem sincronizações").assertExists()
    // The data card only carries the two totals; pending changes live in the
    // summary card above (and are silent when there is nothing to sync).
    composeTestRule.onNodeWithText("Alterações por enviar").assertDoesNotExist()
    composeTestRule.onNodeWithText("Alterações por receber").assertDoesNotExist()
    composeTestRule.onNodeWithText("No servidor").assertExists()
    composeTestRule.onNodeWithText("Na app").assertExists()
    // Only the local total is numeric here; the server row shows a placeholder
    // because the test manager has no server info.
    composeTestRule.onNodeWithText("—").assertExists()
    composeTestRule.onAllNodesWithText("0").assertCountEquals(1)
  }

  @Test
  fun config_mostraNumeroQuandoHaAlteracoesPorReceber() {
    val store = store()
    // Configured, otherwise "Não configurado" takes precedence over the pending state.
    configure(store)
    setContent(
      store,
      manager = syncManagerWithServerInfo(latest = 200L, clientCount = 0),
    )
    composeTestRule.waitForIdle()
    // The summary card alone reports the pending work; the data card has no
    // pending rows any more.
    composeTestRule.onNodeWithText("Alterações por sincronizar").assertExists()
    composeTestRule.onNodeWithText("Novidades por receber").assertExists()
    composeTestRule.onNodeWithText("≥1").assertDoesNotExist()
    composeTestRule.onNodeWithText("Alterações por receber").assertDoesNotExist()
  }

  @Test
  fun config_mostraContagemDoServidorEDaApp() {
    val store = store()
    setContent(
      store,
      clienteCount = flowOf(7),
      manager = syncManagerWithServerInfo(latest = 200L, clientCount = 7),
    )
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("No servidor").assertExists()
    composeTestRule.onNodeWithText("Na app").assertExists()
    // Server total and local total both read 7.
    composeTestRule.onAllNodesWithText("7").assertCountEquals(2)
  }

  @Test
  fun config_mostraTotalDeClientes() {
    val store = store()
    setContent(store, clienteCount = flowOf(3))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Na app").assertExists()
    composeTestRule.onNodeWithText("3").assertExists()
  }

  @Test
  fun config_semConfiguracaoMostraAvisoEImportacaoNoCartaoDoServidor() {
    val store = store()
    setContent(store)
    composeTestRule.waitForIdle()
    // The status title and the server URL row both read "Não configurado".
    composeTestRule.onAllNodesWithText("Não configurado").assertCountEquals(2)
    composeTestRule.onNodeWithText("Não configurada").assertExists()
    composeTestRule
      .onNodeWithText("Importe a configuração do servidor para sincronizar.")
      .assertExists()
    composeTestRule.onNodeWithText("Sincronizar").assertDoesNotExist()
    // The status card only reports that sync is unavailable; the single import
    // action lives in the Servidor card.
    composeTestRule.onNodeWithText("Não disponível").assertIsDisplayed()
    composeTestRule.onNodeWithText("Importar configuração").assertDoesNotExist()
    composeTestRule.onNodeWithText("Importar configuração do servidor").assertExists()
    composeTestRule.onNodeWithText("Remover configuração").assertDoesNotExist()
  }

  @Test
  fun config_comConfiguracaoMostraSincronizado() {
    val store = store()
    configure(store)
    setContent(store)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Sincronizado").assertExists()
  }

  @Test
  fun config_mostraUrlEChaveMascarada() {
    val store = store()
    configure(store)
    setContent(store)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("URL do servidor").assertExists()
    composeTestRule.onNodeWithText("https://db.example.com").assertExists()
    composeTestRule.onNodeWithText("Chave de assinatura").assertExists()
    composeTestRule.onNodeWithText("AAEC", substring = true).assertExists()
    composeTestRule.onNodeWithText("Guardar").assertDoesNotExist()
    // Already configured: importing is no longer offered, only removal.
    composeTestRule.onNodeWithText("Importar configuração do servidor").assertDoesNotExist()
    composeTestRule.onNodeWithText("Remover configuração").assertExists()
  }

  @Test
  fun config_importConfigPreencheUrlEChave() {
    val store = store()
    val serverViewModel = setContent(store)
    val config =
      """{"server_url":"https://db.example.com","private_seed":"AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="}"""
    serverViewModel.importKey(config)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("https://db.example.com").assertExists()
    composeTestRule.onNodeWithText("Chave de assinatura").assertExists()
    composeTestRule.onNodeWithText("AAEC", substring = true).assertExists()
  }

  @Test
  fun config_importMostraUtilizadorDoFicheiro() {
    val store = store()
    val viewModel = setContent(store)

    viewModel.importKey(
      """{"server_url":"https://db.example.com","private_seed":"AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=","user_name":"carlos"}"""
    )
    composeTestRule.waitForIdle()

    // The name comes from the file, so it shows even with no server lookup.
    composeTestRule.onNodeWithText("Utilizador associado").assertExists()
    composeTestRule.onNodeWithText("carlos").assertExists()
  }

  @Test
  fun config_removerConfiguracaoLimpa() {
    val store = store()
    configure(store)
    setContent(store)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Remover configuração").assertExists()
    composeTestRule.onNodeWithText("Remover chave").assertDoesNotExist()
    composeTestRule.onNodeWithText("Remover configuração").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithText("Não configurado").assertCountEquals(2)
    composeTestRule.onNodeWithText("Não configurada").assertExists()
    // Removal restores the import entry point.
    composeTestRule.onNodeWithText("Importar configuração do servidor").assertExists()
    composeTestRule.onNodeWithText("Remover configuração").assertDoesNotExist()
    assertEquals("", store.settings().baseUrl)
    assertEquals("", store.settings().signingKey)
  }

  @Test
  fun config_importInvalidMostraErro() {
    val store = store()
    val serverViewModel = setContent(store)
    serverViewModel.importKey("not-a-config-file")
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("Ficheiro de configuração inválido", substring = true)
      .assertExists()
  }

  /** Compõe o ecrã com os dois view models reais; devolve o do servidor para os testes o manipularem. */
  private fun setContent(
    store: SettingsStore,
    clienteCount: Flow<Int> = flowOf(0),
    manager: SyncManager = syncManager(),
  ): ServerConfigViewModel {
    val syncViewModel = SyncViewModel(manager, clienteCount)
    val serverViewModel = ServerConfigViewModel(store, manager)
    composeTestRule.setContent {
      ElisdbTheme {
        ConfigScreen(
          syncViewModel = syncViewModel,
          serverViewModel = serverViewModel,
          onBack = {},
        )
      }
    }
    return serverViewModel
  }
}
