package com.example.elisdb.ui.form

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.ElisdbApp
import com.example.elisdb.MainNavigation
import com.example.elisdb.data.ClienteDraft
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class NewClientFormNotPrefilledTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun novo_cliente_formNaoFicaPrePreenchidoComDadosAnteriores() {
    val repository = ApplicationProvider.getApplicationContext<ElisdbApp>().container.repository
    runBlocking {
      repository.save(ClienteDraft(nome = "Prefill Seed", latitude = 38.7, longitude = -9.1))
    }

    composeTestRule.setContent { MainNavigation() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Mais opções").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Novo").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Novo cliente").assertIsDisplayed()
    composeTestRule.onNodeWithText("Nome *").performTextInput("Dados Antigos")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Fechar").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Mais opções").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Novo").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Novo cliente").assertIsDisplayed()
    composeTestRule.onNodeWithText("Dados Antigos").assertDoesNotExist()
    composeTestRule.onNodeWithText("Prefill Seed").assertDoesNotExist()
  }
}
