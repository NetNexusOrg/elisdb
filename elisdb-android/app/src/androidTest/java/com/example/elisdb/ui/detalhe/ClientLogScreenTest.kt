package com.example.elisdb.ui.detalhe

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.AuditDetailsDto
import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.remote.AuditFieldDto
import com.example.elisdb.remote.AuditPhotosDto
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.theme.ElisdbTheme
import java.io.File
import java.util.Calendar
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ClientLogScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun cliente(pending: Boolean = false) =
    Cliente(
      id = "5",
      nome = "Maria",
      contrato = "129020",
      latitude = 1.0,
      longitude = 2.0,
      dataCriacao = 0,
      updatedAt = 0,
      pending = pending,
    )

  private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
      clear()
      set(year, month, day, hour, minute, 0)
    }.timeInMillis / 1000

  private fun setContent(
    viewModel: ClientLogViewModel,
  ) {
    composeTestRule.setContent {
      ElisdbTheme { ClientLogScreen(viewModel = viewModel, onBack = {}) }
    }
  }

  @Test
  fun detalhesMostramOsCamposAlterados() {
    val entries =
      listOf(
        AuditEntryDto(
          action = "edit",
          username = "Joao",
          keyId = "k2",
          ts = epoch(2026, Calendar.AUGUST, 17, 9, 5),
          details =
            AuditDetailsDto(
              fields =
                listOf(
                  AuditFieldDto(field = "nome", old = "JOAO", new = "JOAO SILVA"),
                  AuditFieldDto(
                    field = "localizacao",
                    old = "1.000000, 2.000000",
                    new = "1.500000, -2.250000",
                  ),
                )
            ),
        )
      )
    setContent(
      ClientLogViewModel(FakeLogScreenRepository(ClienteComFotos(cliente(), emptyList())), "5") {
        entries
      }
    )
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Nome").assertExists()
    composeTestRule.onNodeWithText("JOAO").assertExists()
    composeTestRule.onNodeWithText("JOAO SILVA").assertExists()
    composeTestRule.onNodeWithText("Localização").assertExists()
    composeTestRule.onNodeWithText("1.500000, -2.250000").assertExists()
  }

  @Test
  fun detalhesMostramAlteracaoDeEstadoComoAcao() {
    val entries =
      listOf(
        AuditEntryDto(
          action = "edit",
          username = "Joao",
          keyId = "k2",
          ts = epoch(2026, Calendar.AUGUST, 17, 9, 10),
          details =
            AuditDetailsDto(
              fields = listOf(AuditFieldDto(field = "ativo", old = "Ativo", new = "Inativo"))
            ),
        )
      )
    setContent(
      ClientLogViewModel(FakeLogScreenRepository(ClienteComFotos(cliente(), emptyList())), "5") {
        entries
      }
    )
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Desativou o cliente").assertExists()
  }

  @Test
  fun detalhesResumemAsFotos() {
    val entries =
      listOf(
        AuditEntryDto(
          action = "edit",
          username = "Joao",
          keyId = "k2",
          ts = epoch(2026, Calendar.AUGUST, 17, 9, 5),
          details =
            AuditDetailsDto(
              photos =
                AuditPhotosDto(
                  added = listOf("f1", "f2"),
                  removed = listOf("f3"),
                  reordered = true,
                )
            ),
        )
      )
    setContent(
      ClientLogViewModel(FakeLogScreenRepository(ClienteComFotos(cliente(), emptyList())), "5") {
        entries
      }
    )
    composeTestRule.waitForIdle()

    // The photos are not stored locally, so only the counts are shown.
    composeTestRule.onNodeWithText("Adicionou 2 fotos").assertExists()
    composeTestRule.onNodeWithText("Removeu 1 foto").assertExists()
    composeTestRule.onNodeWithText("Reordenou as fotos").assertExists()
  }

  @Test
  fun entradaSemDetalhesMostraSoOAutor() {
    val entries =
      listOf(
        AuditEntryDto(
          action = "edit",
          username = "Joao",
          keyId = "k2",
          ts = epoch(2026, Calendar.AUGUST, 17, 9, 5),
        )
      )
    setContent(
      ClientLogViewModel(FakeLogScreenRepository(ClienteComFotos(cliente(), emptyList())), "5") {
        entries
      }
    )
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Editou o cliente").assertExists()
    composeTestRule.onNodeWithText("Nome").assertDoesNotExist()
  }

  @Test
  fun mostraEntradasAgrupadasPorDia() {
    val entries =
      listOf(
        AuditEntryDto(
          action = "create",
          username = "Maria",
          keyId = "k1",
          ts = epoch(2026, Calendar.AUGUST, 16, 14, 30),
        ),
        AuditEntryDto(
          action = "edit",
          username = "Joao",
          keyId = "k2",
          ts = epoch(2026, Calendar.AUGUST, 17, 9, 5),
        ),
      )
    setContent(
      ClientLogViewModel(FakeLogScreenRepository(ClienteComFotos(cliente(), emptyList())), "5") {
        entries
      }
    )
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Log do cliente").assertExists()
    composeTestRule.onNodeWithText("129020").assertExists()
    composeTestRule.onNodeWithText("2 alterações").assertExists()
    composeTestRule.onNodeWithText("16/08/2026").assertExists()
    composeTestRule.onNodeWithText("17/08/2026").assertExists()
    // "Maria" appears twice: as the client name heading and as the entry actor.
    composeTestRule.onAllNodesWithText("Maria").assertCountEquals(2)
    composeTestRule.onNodeWithText("Criou o cliente").assertExists()
    composeTestRule.onNodeWithText("Joao").assertExists()
    composeTestRule.onNodeWithText("Editou o cliente").assertExists()
    composeTestRule.onNodeWithText("14:30").assertExists()
    composeTestRule.onNodeWithText("09:05").assertExists()
  }

  @Test
  fun timestampsUsamFormato24h() {
    val entries =
      listOf(
        AuditEntryDto(
          action = "edit",
          username = "Maria",
          keyId = "k1",
          ts = epoch(2026, Calendar.AUGUST, 16, 21, 45),
        )
      )
    setContent(
      ClientLogViewModel(FakeLogScreenRepository(ClienteComFotos(cliente(), emptyList())), "5") {
        entries
      }
    )
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("21:45").assertExists()
    composeTestRule.onAllNodesWithText("PM", substring = true).assertCountEquals(0)
    composeTestRule.onAllNodesWithText("AM", substring = true).assertCountEquals(0)
  }

  @Test
  fun erroMostraMensagemETentarNovamenteRecarrega() {
    var calls = 0
    val viewModel =
      ClientLogViewModel(FakeLogScreenRepository(ClienteComFotos(cliente(), emptyList())), "5") {
        calls++
        if (calls == 1) throw java.io.IOException("falha simulada")
        listOf(AuditEntryDto(action = "create", username = "Maria", keyId = "k1", ts = 100L))
      }
    setContent(viewModel)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Não foi possível carregar o histórico do cliente.").assertExists()
    composeTestRule.onNodeWithText("Tentar novamente").performClick()
    composeTestRule.waitForIdle()

    assertTrue(calls >= 2)
    composeTestRule.onNodeWithText("Criou o cliente").assertExists()
    composeTestRule.onNodeWithText("Não foi possível carregar o histórico do cliente.").assertDoesNotExist()
  }

  @Test
  fun historicoVazioComPendenteMostraNotaDeSincronizacao() {
    setContent(
      ClientLogViewModel(
        FakeLogScreenRepository(ClienteComFotos(cliente(pending = true), emptyList())),
        "5",
      ) { emptyList() }
    )
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("Este cliente tem alterações por sincronizar. O histórico aparece depois de sincronizar.")
      .assertExists()
  }

  @Test
  fun historicoVazioSemPendenteMostraSemHistorico() {
    setContent(
      ClientLogViewModel(FakeLogScreenRepository(ClienteComFotos(cliente(), emptyList())), "5") {
        emptyList()
      }
    )
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("Ainda não há alterações registadas no servidor para este cliente.")
      .assertExists()
  }
}

private class FakeLogScreenRepository(private val data: ClienteComFotos) : ClienteRepository {
  override val clientes: Flow<List<Cliente>> = flowOf(emptyList())

  override val clientesComFotos: Flow<List<ClienteComFotos>> = flowOf(emptyList())

  override fun observeCliente(id: String): Flow<ClienteComFotos?> = flowOf(data)

  override suspend fun getCliente(id: String): ClienteComFotos? = data

  override suspend fun stagePhoto(source: Uri): Foto = error("Not used in this test")

  override fun createPhotoFile(): File = error("Not used in this test")

  override suspend fun deletePhotoFile(foto: Foto) = Unit

  override suspend fun save(draft: ClienteDraft): String = error("Not used in this test")

  override suspend fun deleteCliente(id: String) = Unit

  override val syncStatus = flowOf(SyncStatus.IDLE)

  override val syncFault = flowOf<SyncFault?>(null)

  override suspend fun syncNow(): SyncStatus = SyncStatus.IDLE
}
