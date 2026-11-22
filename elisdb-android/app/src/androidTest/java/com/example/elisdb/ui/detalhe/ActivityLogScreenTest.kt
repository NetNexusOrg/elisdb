package com.example.elisdb.ui.detalhe

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.elisdb.remote.ActivityPageDto
import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.theme.ElisdbTheme
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActivityLogScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
      clear()
      set(year, month, day, hour, minute, 0)
    }.timeInMillis / 1000

  private fun entry(
    action: String,
    ts: Long,
    clienteId: String,
    clienteNome: String,
  ) =
    AuditEntryDto(
      action = action,
      username = "Maria",
      keyId = "k1",
      ts = ts,
      clienteId = clienteId,
      clienteNome = clienteNome,
    )

  private fun setContent(
    viewModel: ActivityLogViewModel,
    onClienteClick: (String) -> Unit = {},
  ) {
    composeTestRule.setContent {
      ElisdbTheme {
        ActivityLogScreen(viewModel = viewModel, onBack = {}, onClienteClick = onClienteClick)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun mostraAsEntradasMaisRecentesPrimeiroEAgrupadasPorDia() {
    val viewModel =
      ActivityLogViewModel(
        fetchPage = { _, _ ->
          ActivityPageDto(
            entries =
              listOf(
                entry("edit", epoch(2026, Calendar.AUGUST, 17, 9, 5), "c1", "ANA"),
                entry("create", epoch(2026, Calendar.AUGUST, 16, 14, 30), "c2", "BEA"),
              )
          )
        }
      )
    setContent(viewModel)

    val newerDay =
      composeTestRule.onNodeWithText("17/08/2026").fetchSemanticsNode().boundsInRoot.top
    val olderDay =
      composeTestRule.onNodeWithText("16/08/2026").fetchSemanticsNode().boundsInRoot.top
    assertTrue("o dia mais recente aparece primeiro", newerDay < olderDay)
    composeTestRule.onNodeWithText("Alterações").assertExists()
    composeTestRule.onNodeWithText("ANA").assertExists()
    composeTestRule.onNodeWithText("BEA").assertExists()
  }

  @Test
  fun clienteEliminadoNaoNavega() {
    var clicked: String? = null
    val viewModel =
      ActivityLogViewModel(
        fetchPage = { _, _ ->
          ActivityPageDto(
            entries =
              listOf(
                entry("edit", epoch(2026, Calendar.AUGUST, 17, 9, 5), "c1", "ANA"),
                entry("delete", epoch(2026, Calendar.AUGUST, 17, 8, 5), "c2", "BEA"),
              )
          )
        },
        findExistingClienteIds = { setOf("c1") },
      )
    setContent(viewModel) { clicked = it }

    composeTestRule.onNodeWithText("ANA").performClick()
    assertEquals("c1", clicked)

    composeTestRule.onNodeWithText("BEA").performClick()
    assertEquals("c1", clicked)
  }

  @Test
  fun carregarMaisAcrescentaAPaginaSeguinte() {
    val first = entry("edit", epoch(2026, Calendar.AUGUST, 17, 9, 5), "c1", "ANA")
    val second = entry("create", epoch(2026, Calendar.AUGUST, 16, 14, 30), "c2", "BEA")
    val viewModel =
      ActivityLogViewModel(
        fetchPage = { _, beforeId ->
          if (beforeId == null) {
            ActivityPageDto(listOf(first), nextBeforeId = 5)
          } else {
            ActivityPageDto(listOf(second), nextBeforeId = null)
          }
        }
      )
    setContent(viewModel)

    composeTestRule.onNodeWithText("Carregar mais").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("BEA").assertExists()
    composeTestRule.onNodeWithText("Carregar mais").assertDoesNotExist()
  }
}
