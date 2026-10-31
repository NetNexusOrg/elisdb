package com.example.elisdb.ui.about

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.elisdb.BuildConfig
import com.example.elisdb.remote.AppReleaseDto
import com.example.elisdb.theme.ElisdbTheme
import org.junit.Rule
import org.junit.Test

class AboutScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun release(code: Int) =
    AppReleaseDto(
      versionName = "1.6.$code",
      versionCode = code,
      size = 18_000_000,
      sha256 = "abc",
      filename = "elisdb-1.6.$code-$code.apk",
    )

  private fun setContent(
    installedVersionCode: Int = 10,
    isConfigured: Boolean = true,
    fetchRelease: suspend () -> AppReleaseDto? = { null },
  ) {
    val viewModel =
      UpdateViewModel(
        installedVersionCode = installedVersionCode,
        isConfigured = { isConfigured },
        fetchRelease = fetchRelease,
      )
    composeTestRule.setContent {
      ElisdbTheme { AboutScreen(onBack = {}, updateViewModel = viewModel) }
    }
  }

  private fun waitForText(text: String) {
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }
  }

  @Test
  fun sobre_mostraInformacaoDaApp() {
    setContent()
    composeTestRule.onNodeWithText("Sobre").assertExists()
    composeTestRule.onNodeWithText("ElisDB").assertExists()
    composeTestRule
      .onNodeWithText("Versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
      .assertExists()
    composeTestRule.onNodeWithContentDescription("ElisDB").assertExists()
    composeTestRule
      .onNodeWithText("Gestão de clientes offline-first", substring = true)
      .assertDoesNotExist()
  }

  @Test
  fun sobre_naoMostraEquipa() {
    setContent()
    composeTestRule.onNodeWithText("Equipa").assertDoesNotExist()
    composeTestRule.onNodeWithText("Ricardo Pinto").assertDoesNotExist()
    composeTestRule.onNodeWithText("João Galado").assertDoesNotExist()
    composeTestRule.onNodeWithText("Milton Barca").assertDoesNotExist()
    composeTestRule
      .onNodeWithContentDescription("Enviar email para João Galado")
      .assertDoesNotExist()
  }

  @Test
  fun sobre_procuraAtualizacoesEMostraVersaoNova() {
    setContent(fetchRelease = { release(11) })

    composeTestRule.onNodeWithText("Procurar atualizações").performClick()

    waitForText("Versão 1.6.11 (11)")
    composeTestRule.onNodeWithText("Transferir").assertExists()
  }

  @Test
  fun sobre_semVersaoNovaDizQueEstaAtualizada() {
    setContent(fetchRelease = { release(10) })

    composeTestRule.onNodeWithText("Procurar atualizações").performClick()

    waitForText("A app está atualizada")
  }

  @Test
  fun sobre_semServidorConfiguradoNaoProcura() {
    setContent(isConfigured = false)

    composeTestRule.onNodeWithText("Procurar atualizações").performClick()

    waitForText("Configura o servidor para procurar atualizações.")
  }
}
