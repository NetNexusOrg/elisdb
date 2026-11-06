package com.example.elisdb.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.elisdb.sync.ServerAvailability
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.theme.ElisdbTheme
import org.junit.Rule
import org.junit.Test

class SyncStatusBarTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun setBar(
    status: SyncStatus,
    fault: SyncFault? = null,
    availability: ServerAvailability = ServerAvailability.Online,
    hasSynced: Boolean = true,
    detail: String? = null,
    hasPendingUploads: Boolean = false,
    hasPendingDownloads: Boolean = false,
  ) {
    composeTestRule.setContent {
      ElisdbTheme {
        SyncStatusBar(
          syncStatus = status,
          syncFault = fault,
          serverAvailability = availability,
          hasSynced = hasSynced,
          hasPendingUploads = hasPendingUploads,
          hasPendingDownloads = hasPendingDownloads,
          detail = detail,
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun idleComConfig_mostraSincronizado() {
    setBar(SyncStatus.IDLE, availability = ServerAvailability.Online)
    composeTestRule.onNodeWithText("Sincronizado").assertIsDisplayed()
    composeTestRule.onNodeWithText("A sincronizar…").assertDoesNotExist()
  }

  @Test
  fun idleSemSucesso_mostraPorSincronizar() {
    setBar(SyncStatus.IDLE, hasSynced = false)
    composeTestRule.onNodeWithText("Por sincronizar").assertIsDisplayed()
  }

  @Test
  fun idleSemConfig_mostraNaoConfigurado() {
    setBar(SyncStatus.IDLE, availability = ServerAvailability.NotConfigured)
    composeTestRule.onNodeWithText("Não configurado").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sincronizado").assertDoesNotExist()
  }

  @Test
  fun offline_mostraServidorOffline() {
    setBar(SyncStatus.IDLE, availability = ServerAvailability.Offline)
    composeTestRule.onNodeWithText("Indisponível").assertIsDisplayed()
  }

  @Test
  fun inProgress_mostraMensagem() {
    setBar(SyncStatus.IN_PROGRESS)
    composeTestRule.onNodeWithText("A sincronizar…").assertIsDisplayed()
  }

  @Test
  fun success_mostraSincronizado() {
    setBar(SyncStatus.SUCCESS)
    composeTestRule.onNodeWithText("Sincronizado").assertIsDisplayed()
  }

  @Test
  fun errorConfig_mostraConfiguracaoInvalida() {
    setBar(SyncStatus.ERROR, SyncFault.CONFIG)
    composeTestRule.onNodeWithText("Configuração inválida").assertIsDisplayed()
  }

  @Test
  fun errorAuth_mostraMensagemChaveNaoRegistada() {
    setBar(SyncStatus.ERROR, SyncFault.AUTH)
    composeTestRule.onNodeWithText("Chave inválida")
      .assertIsDisplayed()
  }

  @Test
  fun errorClock_mostraMensagemRelogio() {
    setBar(SyncStatus.ERROR, SyncFault.CLOCK)
    composeTestRule.onNodeWithText("Relógio dessincronizado")
      .assertIsDisplayed()
  }

  @Test
  fun errorNetwork_mostraMensagemLigacao() {
    setBar(SyncStatus.ERROR, SyncFault.NETWORK)
    composeTestRule.onNodeWithText("Indisponível").assertIsDisplayed()
  }

  @Test
  fun offlineTemPrioridadeSobreSincronizacaoEmCurso() {
    setBar(SyncStatus.IN_PROGRESS, availability = ServerAvailability.Offline)
    composeTestRule.onNodeWithText("Indisponível").assertIsDisplayed()
    composeTestRule.onNodeWithText("A sincronizar…").assertDoesNotExist()
  }

  @Test
  fun errorComPendencias_mostraErroEmVezDePendente() {
    setBar(SyncStatus.ERROR, SyncFault.AUTH, hasPendingUploads = true)
    composeTestRule.onNodeWithText("Chave inválida")
      .assertIsDisplayed()
    composeTestRule.onNodeWithText("Alterações pendentes").assertDoesNotExist()
  }

  @Test
  fun semAcaoDeVerificacao() {
    // The check-for-updates action was removed; the screen checks on open instead.
    setBar(SyncStatus.SUCCESS)
    composeTestRule.onNodeWithContentDescription("Verificar").assertDoesNotExist()
    composeTestRule.onNodeWithTag("check_action").assertDoesNotExist()
  }

  @Test
  fun pendingUploads_mostraResumoEDetalheDeEnvio() {
    setBar(SyncStatus.IDLE, detail = "3 por enviar", hasPendingUploads = true)
    composeTestRule.onNodeWithText("Alterações pendentes").assertIsDisplayed()
    composeTestRule.onNodeWithText("3 por enviar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sincronizado").assertDoesNotExist()
  }

  @Test
  fun pendingDownloads_mostraResumoEDetalheDeRececao() {
    setBar(SyncStatus.IDLE, detail = "Novidades por receber", hasPendingDownloads = true)
    composeTestRule.onNodeWithText("Alterações pendentes").assertIsDisplayed()
    composeTestRule.onNodeWithText("Novidades por receber").assertIsDisplayed()
  }

  @Test
  fun pendingAmbos_mostraResumoEDetalheCombinado() {
    setBar(
      SyncStatus.IDLE,
      detail = "2 por enviar · novidades por receber",
      hasPendingUploads = true,
      hasPendingDownloads = true,
    )
    composeTestRule.onNodeWithText("Alterações pendentes").assertIsDisplayed()
    composeTestRule.onNodeWithText("2 por enviar · novidades por receber").assertIsDisplayed()
  }

  @Test
  fun pendingOverrideSucessoAntigo() {
    // Pending state wins over a stale success.
    setBar(SyncStatus.SUCCESS, hasPendingUploads = true)
    composeTestRule.onNodeWithText("Alterações pendentes").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sincronizado").assertDoesNotExist()
  }

  @Test
  fun semDetalhe_mostraApenasOTitulo() {
    setBar(SyncStatus.IDLE, hasPendingUploads = true)
    composeTestRule.onNodeWithText("Alterações pendentes").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sincronizado").assertDoesNotExist()
  }
}
