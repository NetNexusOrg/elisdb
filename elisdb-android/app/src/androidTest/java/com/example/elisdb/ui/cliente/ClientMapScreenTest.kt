package com.example.elisdb.ui.cliente

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.theme.ElisdbTheme
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ClientMapScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun mapa_semClientesMostraTituloEEstadoVazio() {
    setMapContent(emptyList())

    composeTestRule.onNodeWithText("Mapa de clientes").assertExists()
    composeTestRule.onNodeWithContentDescription("Voltar").assertExists()
    composeTestRule.onNodeWithText("Ainda não há clientes.").assertExists()
  }

  @Test
  fun mapa_menuDeFiltroMostraOpcoes() {
    setMapContent(emptyList())

    composeTestRule.onNodeWithContentDescription("Filtrar clientes").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Todos os clientes").assertExists()
    composeTestRule.onNodeWithText("Apenas ativos").assertExists()
    composeTestRule.onNodeWithText("Apenas inativos").assertExists()
  }

  @Test
  fun mapa_filtroAtivoSemResultadosMostraMensagemDeFiltro() {
    setMapContent(
      listOf(
        Cliente(
          id = "1",
          nome = "Rui",
          latitude = 1.0,
          longitude = 2.0,
          ativo = false,
          dataCriacao = 0,
          updatedAt = 0,
        )
      )
    )

    composeTestRule.onNodeWithText("Nenhum cliente corresponde ao filtro.").assertExists()
  }

  @Test
  fun mapa_clientesSemCoordenadasNaoMostraContador() {
    setMapContent(
      listOf(
        Cliente(id = "1", nome = "Maria", latitude = null, longitude = null, dataCriacao = 0, updatedAt = 0),
        Cliente(id = "2", nome = "João", latitude = null, longitude = null, dataCriacao = 0, updatedAt = 0),
      )
    )

    composeTestRule.onNodeWithText("Nenhum cliente tem localização definida.").assertExists()
    composeTestRule.onNodeWithText("2 clientes sem localização").assertDoesNotExist()
  }

  private fun setMapContent(clientes: List<Cliente>) {
    val viewModel = ClientMapViewModel(FakeMapScreenRepository(flowOf(clientes)))
    composeTestRule.setContent {
      ElisdbTheme {
        ClientMapScreen(viewModel = viewModel, onClienteClick = {}, onBack = {})
      }
    }
  }
}

private class FakeMapScreenRepository(
  private val clientesFlow: Flow<List<Cliente>>,
) : ClienteRepository {
  override val clientes: Flow<List<Cliente>> = clientesFlow

  override val clientesComFotos: Flow<List<ClienteComFotos>> = flowOf(emptyList())

  override fun observeCliente(id: String): Flow<ClienteComFotos?> = flowOf(null)

  override suspend fun getCliente(id: String): ClienteComFotos? = null

  override suspend fun stagePhoto(source: Uri): Foto = error("Not used in this test")

  override fun createPhotoFile(): File = error("Not used in this test")

  override suspend fun deletePhotoFile(foto: Foto) = Unit

  override suspend fun save(draft: ClienteDraft): String = error("Not used in this test")

  override suspend fun deleteCliente(id: String) = Unit

  override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)

  override val syncFault: Flow<SyncFault?> = flowOf(null)

  override suspend fun syncNow(): SyncStatus = SyncStatus.IDLE
}
