package com.example.elisdb.ui.cliente

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.ClienteDraft
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

class ClientListScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val clientes =
    listOf(
      ClienteComFotos(
        cliente = Cliente(id = "1", nome = "Maria", contrato = "CT-100", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0),
        fotos = emptyList(),
      ),
      ClienteComFotos(
        cliente = Cliente(id = "2", nome = "João", contrato = "CT-200", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0),
        fotos = emptyList(),
      ),
    )

  @Test
  fun lista_pesquisaFiltraLinhas() {
    setListContent(clientes)
    composeTestRule.onNodeWithText("Maria").assertExists()
    composeTestRule.onNodeWithText("João").assertExists()

    composeTestRule.onNodeWithText("Pesquisar clientes…").performTextInput("CT-2")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("João").assertExists()
    composeTestRule.onNodeWithText("Maria").assertDoesNotExist()
  }

  @Test
  fun lista_pesquisaSemCorrespondenciaMostraMensagem() {
    setListContent(clientes)

    composeTestRule.onNodeWithText("Pesquisar clientes…").performTextInput("xyz")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Nenhum cliente corresponde à pesquisa.").assertExists()
    composeTestRule.onNodeWithText("Maria").assertDoesNotExist()
    composeTestRule.onNodeWithText("João").assertDoesNotExist()
  }

  @Test
  fun lista_limparPesquisaRepoeTodaLista() {
    setListContent(clientes)

    composeTestRule.onNodeWithText("Pesquisar clientes…").performTextInput("CT-2")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("João").assertExists()
    composeTestRule.onNodeWithText("Maria").assertDoesNotExist()

    composeTestRule.onNodeWithContentDescription("Limpar pesquisa").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Maria").assertExists()
    composeTestRule.onNodeWithText("João").assertExists()
  }

  @Test
  fun topbar_mostraMenuEAcoes() {
    setListContent(clientes)
    composeTestRule.onNodeWithText("Clientes").assertExists()
    composeTestRule.onNodeWithContentDescription("Mais opções").assertExists()
    composeTestRule.onNodeWithContentDescription("Mais opções").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Novo").assertExists()
    composeTestRule.onNodeWithText("Configuração").assertExists()
    composeTestRule.onNodeWithText("Sobre").assertExists()
  }

  @Test
  fun clienteSemFoto_mostraAvatarGenerico() {
    val semFoto =
      listOf(
        ClienteComFotos(
          cliente = Cliente(id = "1", nome = "Maria", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0),
          fotos = emptyList(),
        ),
      )
    setListContent(semFoto)
    composeTestRule.onNodeWithText("Maria").assertExists()
    composeTestRule.onNodeWithTag("generic_avatar", useUnmergedTree = true).assertExists()
  }

  @Test
  fun clienteComFoto_naoMostraAvatarGenerico() {
    val comFoto =
      listOf(
        ClienteComFotos(
          cliente = Cliente(id = "2", nome = "João", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0),
          fotos = listOf(Foto(id = "f1", clienteId = "2", uriLocal = "file:/nao-existe.jpg", mime = "image/jpeg", ordem = 0)),
        ),
      )
    setListContent(comFoto)
    composeTestRule.onNodeWithText("João").assertExists()
    composeTestRule.onNodeWithTag("generic_avatar", useUnmergedTree = true).assertDoesNotExist()
  }

  private fun setListContent(clientes: List<ClienteComFotos>) {
    val repository = FakeListRepository(clientes)
    val viewModel = ClientListViewModel(repository)
    composeTestRule.setContent {
      ElisdbTheme {
        ClientListScreen(viewModel = viewModel, onClienteClick = {}, onAddClick = {})
      }
    }
  }
}

private class FakeListRepository(
  private val data: List<ClienteComFotos>,
) : ClienteRepository {
  override val clientes: Flow<List<Cliente>> = flowOf(emptyList())

  override val clientesComFotos: Flow<List<ClienteComFotos>> = flowOf(data)

  override fun observeCliente(id: String): Flow<ClienteComFotos?> =
    flowOf(data.firstOrNull { it.cliente.id == id })

  override suspend fun getCliente(id: String): ClienteComFotos? =
    data.firstOrNull { it.cliente.id == id }

  override suspend fun stagePhoto(source: Uri): Foto = error("Not used in this test")

  override fun createPhotoFile(): File = error("Not used in this test")

  override suspend fun deletePhotoFile(foto: Foto) = Unit

  override suspend fun save(draft: ClienteDraft): String = error("Not used in this test")

  override suspend fun deleteCliente(id: String) = Unit

  override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)

  override val syncFault: Flow<SyncFault?> = flowOf(null)

  override suspend fun syncNow(): SyncStatus = SyncStatus.IDLE
}
