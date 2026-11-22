package com.example.elisdb.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.elisdb.theme.ElisdbTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DecisionDialogTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun showDialog(
    dismissLabel: String = "Cancelar",
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      ElisdbTheme {
        DecisionDialog(
          title = "Apagar cliente",
          text = "Tens a certeza?",
          confirmLabel = "Apagar",
          confirmColor = MaterialTheme.colorScheme.error,
          dismissLabel = dismissLabel,
          onConfirm = onConfirm,
          onDismissRequest = onDismiss,
        )
      }
    }
  }

  @Test
  fun dialogo_cancelarFicaAEscquerdaDaAcao() {
    showDialog()

    val cancel = composeTestRule.onNodeWithTag("decision_dialog_dismiss").getUnclippedBoundsInRoot()
    val confirm = composeTestRule.onNodeWithTag("decision_dialog_confirm").getUnclippedBoundsInRoot()

    assertTrue(cancel.left < confirm.left)
    assertTrue(cancel.right <= confirm.left)
  }

  @Test
  fun dialogo_cancelarFechaSemConfirmar() {
    var confirmed = false
    var dismissed = false
    showDialog(onConfirm = { confirmed = true }, onDismiss = { dismissed = true })

    composeTestRule.onNodeWithTag("decision_dialog_dismiss").performClick()

    assertTrue(dismissed)
    assertFalse(confirmed)
  }

  @Test
  fun dialogo_confirmarChamaAAcao() {
    var confirmed = false
    var dismissed = false
    showDialog(onConfirm = { confirmed = true }, onDismiss = { dismissed = true })

    composeTestRule.onNodeWithTag("decision_dialog_confirm").performClick()

    assertTrue(confirmed)
    assertFalse(dismissed)
  }

  @Test
  fun dialogo_usaORotuloDeAdiar() {
    showDialog(dismissLabel = "Adiar")

    composeTestRule.onNodeWithText("Adiar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancelar").assertDoesNotExist()
  }
}
