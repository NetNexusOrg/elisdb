package com.example.elisdb.ui.about

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.elisdb.remote.AppReleaseDto
import com.example.elisdb.sync.ServerAvailability
import com.example.elisdb.theme.ElisdbTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppUpdatePromptTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun release(code: Int) =
    AppReleaseDto(
      versionName = "1.13.$code",
      versionCode = code,
      size = 18_000_000,
      sha256 = "abc",
      filename = "elisdb-1.13.$code-$code.apk",
    )

  private fun setContent(
    isAboutOpen: Boolean = false,
    onUpdate: () -> Unit = {},
  ) {
    val availability = MutableStateFlow(ServerAvailability.Online)
    val viewModel =
      UpdateViewModel(
        installedVersionCode = 10,
        isConfigured = { true },
        fetchRelease = { release(11) },
        serverAvailability = availability,
      )
    composeTestRule.setContent {
      ElisdbTheme {
        AppUpdatePrompt(
          updateViewModel = viewModel,
          serverAvailability = availability,
          isAboutOpen = isAboutOpen,
          onUpdate = onUpdate,
        )
      }
    }
  }

  private fun waitForPrompt() {
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule
        .onAllNodesWithText("Atualização disponível")
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
  }

  @Test
  fun aviso_mostraVersaoNovaEAcoes() {
    setContent()

    waitForPrompt()
    composeTestRule.onNodeWithText("Atualização disponível").assertExists()
    composeTestRule.onNodeWithText("Atualizar").assertExists()
    composeTestRule.onNodeWithText("Adiar").assertExists()
  }

  @Test
  fun aviso_adiarFechaSemNavegar() {
    var updated = false
    setContent(onUpdate = { updated = true })

    waitForPrompt()
    composeTestRule.onNodeWithText("Adiar").performClick()

    composeTestRule.onNodeWithText("Atualização disponível").assertDoesNotExist()
    assertFalse(updated)
  }

  @Test
  fun aviso_atualizarChamaNavegacao() {
    var updated = false
    setContent(onUpdate = { updated = true })

    waitForPrompt()
    composeTestRule.onNodeWithText("Atualizar").performClick()

    assertTrue(updated)
    composeTestRule.onNodeWithText("Atualização disponível").assertDoesNotExist()
  }

  @Test
  fun aviso_naoApareceQuandoOSobreJaEstaAberto() {
    setContent(isAboutOpen = true)

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Atualização disponível").assertDoesNotExist()
  }
}
