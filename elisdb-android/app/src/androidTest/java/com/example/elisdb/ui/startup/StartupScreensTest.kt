package com.example.elisdb.ui.startup

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.elisdb.theme.ElisdbTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartupScreensTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun setup_mostraInformacaoEAbreConfiguracao() {
    var opened = false
    composeTestRule.setContent {
      ElisdbTheme {
        SetupRequiredScreen(missingUserName = false, onOpenConfig = { opened = true })
      }
    }

    composeTestRule.onNodeWithText("Acesso Negado").assertIsDisplayed()
    composeTestRule
      .onNodeWithText("app de dados privados", substring = true)
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("<utilizador>.config.json", substring = true)
      .assertIsDisplayed()
    composeTestRule.onNodeWithText("Abrir configuração").performClick()

    assertTrue(opened)
  }

  @Test
  fun setup_avisaQuandoFaltaNome() {
    composeTestRule.setContent {
      ElisdbTheme {
        SetupRequiredScreen(missingUserName = true, onOpenConfig = {})
      }
    }

    composeTestRule
      .onNodeWithText("falta o nome do utilizador", substring = true)
      .assertIsDisplayed()
  }
}
