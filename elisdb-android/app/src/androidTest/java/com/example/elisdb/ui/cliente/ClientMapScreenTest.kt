package com.example.elisdb.ui.cliente

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import com.example.elisdb.sync.SyncFault
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    composeTestRule.onNodeWithText("Estado").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Todos os clientes").assertExists()
    composeTestRule.onNodeWithText("Apenas ativos").assertExists()
    composeTestRule.onNodeWithText("Apenas inativos").assertExists()
  }

  @Test
  fun mapa_menuMostraTagsDisponiveis() {
    setMapContent(
      listOf(
        Cliente(id = "1", nome = "Maria", tag = "PREMIUM", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0),
        Cliente(id = "2", nome = "João", tag = "GOLD", latitude = 3.0, longitude = 4.0, dataCriacao = 0, updatedAt = 0),
      )
    )

    composeTestRule.onNodeWithContentDescription("Filtrar clientes").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Tags").assertExists()
    composeTestRule.onNodeWithText("Todas as tags").assertExists()
    composeTestRule.onNodeWithText("GOLD").assertExists()
    composeTestRule.onNodeWithText("PREMIUM").assertExists()
  }

  @Test
  fun mapa_selecionarTagMantemMenuAberto() {
    setMapContent(
      listOf(
        Cliente(id = "1", nome = "Maria", tag = "PREMIUM", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0),
        Cliente(id = "2", nome = "João", tag = "GOLD", latitude = 3.0, longitude = 4.0, dataCriacao = 0, updatedAt = 0),
      )
    )

    composeTestRule.onNodeWithContentDescription("Filtrar clientes").performClick()
    composeTestRule.onNodeWithText("PREMIUM").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("GOLD").assertExists()
    composeTestRule.onNodeWithText("Estado").assertExists()
    composeTestRule.onNodeWithText("Todas as tags").assertExists()
  }

  @Test
  fun mapa_voltarDoSubmenuEstadoMostraMenuPrincipal() {
    setMapContent(emptyList())

    composeTestRule.onNodeWithContentDescription("Filtrar clientes").performClick()
    composeTestRule.onNodeWithText("Estado").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("filter_menu_back").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Estado").assertExists()
    composeTestRule.onNodeWithText("Apenas ativos").assertDoesNotExist()
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
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      val filtro by viewModel.filtro.collectAsStateWithLifecycle()
      val tagsFiltro by viewModel.tagsFiltro.collectAsStateWithLifecycle()
      ElisdbTheme {
        ClientMapScreen(
          state = state,
          filtro = filtro,
          tagsFiltro = tagsFiltro,
          onFiltroChange = viewModel::onFiltroChange,
          onTagToggle = viewModel::onTagToggle,
          onTagsClear = viewModel::onTagsClear,
          onClienteClick = {},
          onBack = {},
        )
      }
    }
  }
}

private class FakeMapScreenRepository(
  private val clientesFlow: Flow<List<Cliente>>,
) : com.example.elisdb.EmptyCatalogoRepository() {
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
