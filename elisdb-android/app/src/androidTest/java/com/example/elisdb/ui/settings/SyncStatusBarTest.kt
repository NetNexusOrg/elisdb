package com.example.elisdb.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.theme.ElisdbTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SyncStatusBarTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun setBar(
    status: SyncStatus,
    fault: SyncFault? = null,
    hasConfig: Boolean = true,
    checking: Boolean = false,
    onCheckClick: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      ElisdbTheme {
        SyncStatusBar(
          syncStatus = status,
          syncFault = fault,
          hasConfig = hasConfig,
          checking = checking,
          onCheckClick = onCheckClick,
        )
      }
    }
  }

  @Test
  fun idleComConfig_mostraSincronizadoEVerificar() {
    setBar(SyncStatus.IDLE, hasConfig = true)
    composeTestRule.onNodeWithContentDescription("Verificar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sincronizado").assertIsDisplayed()
    composeTestRule.onNodeWithText("A sincronizar…").assertDoesNotExist()
  }

  @Test
  fun idleSemConfig_mostraNaoConfiguradoEDesativaVerificar() {
    setBar(SyncStatus.IDLE, hasConfig = false)
    composeTestRule.onNodeWithText("Não configurado").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sincronizado").assertDoesNotExist()
    composeTestRule.onNodeWithTag("check_action").assertIsNotEnabled()
  }

  @Test
  fun inProgress_mostraMensagemAManteraVerificar() {
    setBar(SyncStatus.IN_PROGRESS)
    composeTestRule.onNodeWithText("A sincronizar…").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Verificar").assertIsDisplayed()
  }

  @Test
  fun success_mostraSincronizadoEVerificar() {
    setBar(SyncStatus.SUCCESS)
    composeTestRule.onNodeWithText("Sincronizado").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Verificar").assertIsDisplayed()
  }

  @Test
  fun errorConfig_mostraNaoConfigurado() {
    setBar(SyncStatus.ERROR, SyncFault.CONFIG)
    composeTestRule.onNodeWithText("Não configurado").assertIsDisplayed()
  }

  @Test
  fun errorAuth_mostraMensagemChaveNaoRegistada() {
    setBar(SyncStatus.ERROR, SyncFault.AUTH)
    composeTestRule.onNodeWithText("Servidor rejeitou a sincronização (chave não registada).")
      .assertIsDisplayed()
  }

  @Test
  fun errorClock_mostraMensagemRelogio() {
    setBar(SyncStatus.ERROR, SyncFault.CLOCK)
    composeTestRule.onNodeWithText("Relógio do dispositivo dessincronizado. Verifique a hora do dispositivo.")
      .assertIsDisplayed()
  }

  @Test
  fun errorNetwork_mostraMensagemLigacao() {
    setBar(SyncStatus.ERROR, SyncFault.NETWORK)
    composeTestRule.onNodeWithText("Falha na sincronização. Verifique a ligação.").assertIsDisplayed()
  }

  @Test
  fun cliqueNoVerificarInvocaCallback() {
    var clicked = false
    setBar(SyncStatus.IDLE, onCheckClick = { clicked = true })
    composeTestRule.onNodeWithContentDescription("Verificar").performClick()
    composeTestRule.waitForIdle()
    assert(clicked)
  }

  @Test
  fun checking_mostraSpinnerEDesativaVerificar() {
    setBar(SyncStatus.IDLE, checking = true)
    composeTestRule.onNodeWithContentDescription("Verificar").assertDoesNotExist()
    composeTestRule.onNodeWithTag("check_action").assertIsNotEnabled()
  }

  @Test
  fun pendingUploads_mostraDisponivelEnviar() {
    pendingBar(hasPendingUploads = true)
    composeTestRule.onNodeWithText("Sincronização disponível (enviar)").assertIsDisplayed()
  }

  @Test
  fun pendingDownloads_mostraDisponivelReceber() {
    pendingBar(hasPendingDownloads = true)
    composeTestRule.onNodeWithText("Sincronização disponível (receber)").assertIsDisplayed()
  }

  @Test
  fun pendingAmbos_mostraDisponivelEnviarEReceber() {
    pendingBar(hasPendingUploads = true, hasPendingDownloads = true)
    composeTestRule.onNodeWithText("Sincronização disponível (enviar e receber)").assertIsDisplayed()
  }

  @Test
  fun pendingOverrideSucessoAntigo() {
    // Pending state wins over a stale success.
    pendingBar(status = SyncStatus.SUCCESS, hasPendingUploads = true)
    composeTestRule.onNodeWithText("Sincronização disponível (enviar)").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sincronizado").assertDoesNotExist()
  }

  private fun pendingBar(
    status: SyncStatus = SyncStatus.IDLE,
    hasPendingUploads: Boolean = false,
    hasPendingDownloads: Boolean = false,
  ) {
    composeTestRule.setContent {
      ElisdbTheme {
        SyncStatusBar(
          syncStatus = status,
          syncFault = null,
          hasPendingUploads = hasPendingUploads,
          hasPendingDownloads = hasPendingDownloads,
          onCheckClick = {},
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  private fun actionLeftEdge(): Dp =
    composeTestRule.onNodeWithTag("check_action").getBoundsInRoot().left

  @Test
  fun acaoVerificar_mantemBordaEsquerdaFixaEmTodosEstados() {
    val status = mutableStateOf(SyncStatus.IDLE)
    val fault = mutableStateOf<SyncFault?>(null)
    setContent(status, fault, mutableStateOf(false))
    val idleLeft = actionLeftEdge()
    status.value = SyncStatus.IN_PROGRESS
    composeTestRule.waitForIdle()
    val inProgressLeft = actionLeftEdge()
    status.value = SyncStatus.SUCCESS
    composeTestRule.waitForIdle()
    val successLeft = actionLeftEdge()
    status.value = SyncStatus.ERROR
    fault.value = SyncFault.CONFIG
    composeTestRule.waitForIdle()
    val errorLeft = actionLeftEdge()
    assertEquals(idleLeft, inProgressLeft)
    assertEquals(idleLeft, successLeft)
    assertEquals(idleLeft, errorLeft)
  }

  @Test
  fun acaoVerificar_ficaADireitaDaMensagem() {
    setContent(mutableStateOf(SyncStatus.SUCCESS), mutableStateOf<SyncFault?>(null), mutableStateOf(false))
    val messageRight = composeTestRule.onNodeWithText("Sincronizado").getBoundsInRoot().right
    assertTrue(actionLeftEdge() >= messageRight)
  }

  @Test
  fun spinnerDaVerificacao_ficaNaPosicaoDaAcao() {
    val checking = mutableStateOf(false)
    setContent(mutableStateOf(SyncStatus.IDLE), mutableStateOf<SyncFault?>(null), checking)
    val idleLeft = actionLeftEdge()
    checking.value = true
    composeTestRule.waitForIdle()
    assertEquals(idleLeft, actionLeftEdge())
  }

  private fun setContent(
    status: androidx.compose.runtime.MutableState<SyncStatus>,
    fault: androidx.compose.runtime.MutableState<SyncFault?>,
    checking: androidx.compose.runtime.MutableState<Boolean>,
  ) {
    composeTestRule.setContent {
      ElisdbTheme {
        SyncStatusBar(
          syncStatus = status.value,
          syncFault = fault.value,
          checking = checking.value,
          onCheckClick = {},
        )
      }
    }
    composeTestRule.waitForIdle()
  }
}
