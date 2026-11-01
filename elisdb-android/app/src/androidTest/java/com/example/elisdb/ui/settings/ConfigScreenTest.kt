package com.example.elisdb.ui.settings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.settings.PrefsSettingsStore
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.sync.PullWatermark
import com.example.elisdb.sync.ServerInfo
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.theme.ElisdbTheme
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
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

          override suspend fun clear() = Unit
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
  fun config_cartaoDadosVemAntesDoCartaoDeSincronizacao() {
    val store = store()
    configure(store)
    setContent(store)
    composeTestRule.waitForIdle()
    val dadosTop = composeTestRule.onNodeWithText("Dados").fetchSemanticsNode().boundsInRoot.top
    val sincronizacaoTop =
      composeTestRule.onNodeWithText("Sincronização").fetchSemanticsNode().boundsInRoot.top
    assertTrue("Dados deve ser o primeiro cartão", dadosTop < sincronizacaoTop)
    composeTestRule.onNodeWithText("Sincronização e servidor").assertDoesNotExist()
    composeTestRule.onNodeWithText("Servidor").assertDoesNotExist()
  }

  @Test
  fun config_mostraUltimaSincronizacaoEDados() {
    val store = store()
    configure(store)
    setContent(store)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Ainda sem sincronizações").assertExists()
    // The data card only carries the two totals; pending changes live in the
    // combined sync/server card below (and are silent when there is nothing to sync).
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
  fun config_mostraCarregamentoDoServidorDuranteOSync() = runBlocking {
    val gate = CompletableDeferred<Unit>()
    val manager =
      SyncManager(
        SyncCoordinatorProvider { throw RuntimeException("not reached") },
        pullWatermark =
          object : PullWatermark {
            override suspend fun get(): Long = 100L

            override suspend fun set(value: Long) = Unit

            override suspend fun clear() = Unit
          },
        fetchServerInfo = {
          gate.await()
          ServerInfo(100L, 0)
        },
      )
    val store = store()
    configure(store)
    val run = launch(Dispatchers.Default) { manager.sync() }
    while (manager.status.value != SyncStatus.IN_PROGRESS) delay(5)
    setContent(store, manager = manager)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("A carregar…").assertExists()
    gate.complete(Unit)
    run.join()
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
    // The combined sync/server card alone reports the pending work; the data card
    // has no pending rows any more.
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
  fun config_semConfiguracaoMostraAvisoEImportacaoNoCartaoDeSincronizacao() {
    val store = store()
    setContent(store)
    composeTestRule.waitForIdle()
    // Only the server URL row reads "Não configurado"; the card headers carry a
    // state pill instead of repeating the same sentence.
    composeTestRule.onNodeWithText("Não configurado").assertExists()
    composeTestRule.onNodeWithText("Por configurar").assertExists()
    composeTestRule.onNodeWithText("Não configurada").assertExists()
    composeTestRule
      .onNodeWithText("Importa o ficheiro de credencial do utilizador para começares a sincronizar.")
      .assertExists()
    composeTestRule.onNodeWithText("Sincronizar").assertDoesNotExist()
    composeTestRule.onNodeWithText("Não disponível").assertDoesNotExist()
    composeTestRule.onNodeWithText("Importar configuração do servidor").assertDoesNotExist()
    composeTestRule.onNodeWithText("Importar credencial do utilizador").assertExists()
    composeTestRule.onNodeWithText("Remover credencial").assertDoesNotExist()
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
    composeTestRule.onNodeWithText("Importar credencial do utilizador").assertDoesNotExist()
    composeTestRule.onNodeWithText("Remover credencial").assertExists()
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
  fun config_removerCredencialLimpa() {
    val store = store()
    configure(store)
    setContent(store)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Remover credencial").assertExists()
    composeTestRule.onNodeWithText("Remover chave").assertDoesNotExist()
    composeTestRule.onNodeWithText("Remover credencial").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    // Removing the credential is destructive, so it asks for confirmation.
    composeTestRule.onNodeWithText("Remover credencial?").assertExists()
    composeTestRule.onNodeWithText("Remover").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Não configurado").assertExists()
    composeTestRule.onNodeWithText("Não configurada").assertExists()
    // Removal restores the import entry point.
    composeTestRule.onNodeWithText("Importar credencial do utilizador").assertExists()
    composeTestRule.onNodeWithText("Remover credencial").assertDoesNotExist()
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
      .onNodeWithText("Ficheiro de credencial inválido", substring = true)
      .assertExists()
  }

  /** Compõe o ecrã com os dois view models reais; devolve o do servidor para os testes o manipularem. */
  @Test
  fun config_mostraAvisoDeRelogioComAcaoCorrigir() {
    var corrected = 0
    setContent(store(), skew = -300L, onCorrectClock = { corrected++ })
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Corrigir").assertExists()
    composeTestRule
      .onNodeWithText(
        "Relógio do dispositivo 5 min atrasado. As alterações podem não chegar aos outros dispositivos."
      )
      .assertExists()
    composeTestRule.onNodeWithText("Corrigir").performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, corrected)
  }

  @Test
  fun config_semDesvioNaoMostraAvisoDeRelogio() {
    setContent(store(), skew = 30L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Corrigir").assertDoesNotExist()
  }

  private fun setContent(
    store: SettingsStore,
    clienteCount: Flow<Int> = flowOf(0),
    manager: SyncManager = syncManager(),
    skew: Long? = null,
    onCorrectClock: suspend () -> Unit = {},
  ): ServerConfigViewModel {
    val syncViewModel =
      SyncViewModel(
        manager,
        clienteCount,
        clockSkewSeconds = flowOf(skew),
        onCorrectClock = onCorrectClock,
      )
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
