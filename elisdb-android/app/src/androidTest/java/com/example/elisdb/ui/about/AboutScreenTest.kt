package com.example.elisdb.ui.about

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.elisdb.BuildConfig
import com.example.elisdb.theme.ElisdbTheme
import org.junit.Rule
import org.junit.Test

class AboutScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun sobre_mostraInformacaoDaApp() {
    composeTestRule.setContent { ElisdbTheme { AboutScreen(onBack = {}) } }
    composeTestRule.onNodeWithText("Sobre").assertExists()
    composeTestRule.onNodeWithText("ElisDB").assertExists()
    composeTestRule
      .onNodeWithText("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
      .assertExists()
    composeTestRule.onNodeWithText("Gestão de clientes offline-first", substring = true).assertExists()
  }

  @Test
  fun sobre_mostraInformacaoDoDesenvolvedor() {
    composeTestRule.setContent { ElisdbTheme { AboutScreen(onBack = {}) } }
    composeTestRule.onNodeWithText("Desenvolvedor").assertExists()
    composeTestRule.onNodeWithText("RP").assertExists()
    composeTestRule.onNodeWithText("kadu247@protonmail.com").assertExists()
  }
}
