package com.example.elisdb.ui.settings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.settings.PrefsSettingsStore
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.sync.SyncCoordinatorProvider
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.theme.ElisdbTheme
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class ServerConfigScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private fun syncManager() =
    SyncManager(
      SyncCoordinatorProvider {
        throw RuntimeException("no network in server config screen test")
      },
    )

  private fun store(): SettingsStore {
    context.deleteSharedPreferences("elisdb_settings")
    return PrefsSettingsStore(context)
  }

  @Test
  fun server_mostraBotaoImportarConfiguracao() {
    val store = store()
    val vm = ServerConfigViewModel(store, syncManager())
    setContent(vm)
    composeTestRule.onNodeWithText("Importar configuração do servidor").assertExists()
    composeTestRule.onNodeWithText("Guardar").assertDoesNotExist()
  }

  @Test
  fun server_urlApareceComoTextoNaoEditavel() {
    val store = store()
    store.save("https://db.example.com", "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=")
    val vm = ServerConfigViewModel(store, syncManager())
    setContent(vm)
    composeTestRule.onNodeWithText("URL do servidor").assertExists()
    composeTestRule.onNodeWithText("https://db.example.com").assertExists()
  }

  @Test
  fun server_importConfigPreencheUrlEChave() {
    val store = store()
    val vm = ServerConfigViewModel(store, syncManager())
    setContent(vm)
    val config =
      """{"server_url":"https://db.example.com","private_seed":"AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="}"""
    vm.importKey(config)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("https://db.example.com").assertExists()
    composeTestRule.onNodeWithText("Chave de assinatura").assertExists()
    composeTestRule.onNodeWithText("AAEC", substring = true).assertExists()
  }

  @Test
  fun server_mostraBotaoRemoverConfiguracaoELimpa() {
    val store = store()
    store.save("https://db.example.com", "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=")
    val vm = ServerConfigViewModel(store, syncManager())
    setContent(vm)
    composeTestRule.onNodeWithText("Remover configuração").assertExists()
    composeTestRule.onNodeWithText("Remover chave").assertDoesNotExist()
    composeTestRule.onNodeWithText("Remover configuração").performClick()
    composeTestRule.waitForIdle()
    assertTrue(composeTestRule.onAllNodesWithText("Não configurado").fetchSemanticsNodes().isNotEmpty())
    composeTestRule.onNodeWithText("Não configurada").assertExists()
    assertEquals("", store.settings().baseUrl)
    assertEquals("", store.settings().signingKey)
  }

  @Test
  fun server_importInvalidMostraErro() {
    val store = store()
    val vm = ServerConfigViewModel(store, syncManager())
    setContent(vm)
    vm.importKey("not-a-config-file")
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("Ficheiro de configuração inválido", substring = true)
      .assertExists()
  }

  private fun setContent(vm: ServerConfigViewModel) {
    composeTestRule.setContent {
      ElisdbTheme {
        ServerConfigScreen(viewModel = vm, onBack = {})
      }
    }
  }
}
