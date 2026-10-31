package com.example.elisdb.ui.detalhe

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.geometry.Offset
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import com.example.elisdb.theme.ElisdbTheme
import java.io.File
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ClientDetailScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun detalhe_mostraContratoAcimaDoNome() {
    val cliente = Cliente(id = "1", nome = "Maria", contrato = "CT-123", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    setDetailContent(cliente)

    composeTestRule.onNodeWithText("CT-123").assertExists()
    val contentNameTop =
      composeTestRule.onAllNodesWithText("Maria").fetchSemanticsNodes().maxOf { it.boundsInRoot.top }
    val contractTop = composeTestRule.onNodeWithText("CT-123").fetchSemanticsNode().boundsInRoot.top
    assertTrue("Contrato deveria aparecer acima do nome", contractTop < contentNameTop)
  }

  @Test
  fun detalhe_semContratoNaoMostraNadaAcimaDoNome() {
    val cliente = Cliente(id = "2", nome = "João", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    setDetailContent(cliente)

    val nodesJoao = composeTestRule.onAllNodesWithText("João").fetchSemanticsNodes()
    assertTrue("João deveria aparecer apenas no conteúdo", nodesJoao.size == 1)
  }

  @Test
  fun detalhe_semLocalizacaoNaoMostraMapaNemNavegacao() {
    val cliente = Cliente(id = "3", nome = "Ana", latitude = null, longitude = null, dataCriacao = 0, updatedAt = 0)
    setDetailContent(cliente)

    composeTestRule.onNodeWithText("Navegar até aqui").assertDoesNotExist()
    composeTestRule.onNodeWithText("Sem localização definida.").assertExists()
    composeTestRule.onNodeWithContentDescription("Navegar até aqui").assertDoesNotExist()
  }

  @Test
  fun log_acaoAbrePaginaDeLogComClienteId() {
    val cliente = Cliente(id = "4", nome = "Beatriz", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val repository = FakeDetailRepository(ClienteComFotos(cliente = cliente, fotos = emptyList()))
    val viewModel = ClientDetailViewModel(repository, cliente.id)
    var openedId: String? = null
    composeTestRule.setContent {
      ElisdbTheme {
        ClientDetailScreen(
          viewModel = viewModel,
          onBack = {},
          onEdit = {},
          onViewLog = { id -> openedId = id },
          onDeleted = {},
          onPhotoClick = {},
          onOpenFullscreenMap = {},
        )
      }
    }

    composeTestRule.onNodeWithContentDescription("Mais opções").performClick()
    composeTestRule.onNodeWithText("Log").performClick()
    composeTestRule.waitForIdle()

    assertEquals("4", openedId)
    // The log is a page now, not the old dialog.
    composeTestRule.onNodeWithText("Fechar").assertDoesNotExist()
  }

  @Test
  fun detalhe_tocarFotoAbreViewerComIndiceCorreto() {
    val cliente = Cliente(id = "9", nome = "Gustavo", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val fotos =
      listOf(
        Foto(id = "f1", clienteId = cliente.id, uriLocal = "/f1.jpg"),
        Foto(id = "f2", clienteId = cliente.id, uriLocal = "/f2.jpg"),
      )
    val repository = FakeDetailRepository(ClienteComFotos(cliente = cliente, fotos = fotos))
    val viewModel = ClientDetailViewModel(repository, cliente.id)
    var clickedIndex = -1
    composeTestRule.setContent {
      ElisdbTheme {
        ClientDetailScreen(
          viewModel = viewModel,
          onBack = {},
          onEdit = {},
          onViewLog = {},
          onDeleted = {},
          onPhotoClick = { clickedIndex = it },
          onOpenFullscreenMap = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithContentDescription("Foto")[1].performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, clickedIndex)
  }

  @Test
  fun photoViewer_swipeEsquerdaMostraFotoSeguinte() {
    val cliente = Cliente(id = "10", nome = "Hugo", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val fotos =
      listOf(
        Foto(id = "f1", clienteId = cliente.id, uriLocal = "/f1.jpg"),
        Foto(id = "f2", clienteId = cliente.id, uriLocal = "/f2.jpg"),
      )
    val repository = FakeDetailRepository(ClienteComFotos(cliente = cliente, fotos = fotos))
    val viewModel = PhotoViewerViewModel(repository, cliente.id, initialIndex = 0)
    composeTestRule.setContent {
      ElisdbTheme {
        PhotoViewerScreen(viewModel = viewModel, onBack = {})
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("1 / 2").assertExists()
    composeTestRule.onNodeWithContentDescription("Foto").performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("2 / 2").assertExists()
  }

  @Test
  fun photoViewer_pinchaNaoMudaDeFoto() {
    val cliente = Cliente(id = "11", nome = "Inês", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val fotos =
      listOf(
        Foto(id = "f1", clienteId = cliente.id, uriLocal = "/f1.jpg"),
        Foto(id = "f2", clienteId = cliente.id, uriLocal = "/f2.jpg"),
      )
    val repository = FakeDetailRepository(ClienteComFotos(cliente = cliente, fotos = fotos))
    val viewModel = PhotoViewerViewModel(repository, cliente.id, initialIndex = 0)
    composeTestRule.setContent {
      ElisdbTheme {
        PhotoViewerScreen(viewModel = viewModel, onBack = {})
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("1 / 2").assertExists()
    composeTestRule.onNodeWithContentDescription("Foto").performTouchInput {
      pinch(
        start0 = center - Offset(50f, 0f),
        end0 = center - Offset(200f, 0f),
        start1 = center + Offset(50f, 0f),
        end1 = center + Offset(200f, 0f),
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("1 / 2").assertExists()
  }

  @Test
  fun photoViewer_arrastoComZoomFazPanNaoSwipe() {
    val cliente = Cliente(id = "12", nome = "Joana", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val fotos =
      listOf(
        Foto(id = "f1", clienteId = cliente.id, uriLocal = "/f1.jpg"),
        Foto(id = "f2", clienteId = cliente.id, uriLocal = "/f2.jpg"),
      )
    val repository = FakeDetailRepository(ClienteComFotos(cliente = cliente, fotos = fotos))
    val viewModel = PhotoViewerViewModel(repository, cliente.id, initialIndex = 0)
    composeTestRule.setContent {
      ElisdbTheme {
        PhotoViewerScreen(viewModel = viewModel, onBack = {})
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("1 / 2").assertExists()
    composeTestRule.onNodeWithContentDescription("Foto").performTouchInput {
      pinch(
        start0 = center - Offset(50f, 0f),
        end0 = center - Offset(200f, 0f),
        start1 = center + Offset(50f, 0f),
        end1 = center + Offset(200f, 0f),
      )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Foto").performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("1 / 2").assertExists()
  }

  @Test
  fun photoViewer_arrastoComZoomDeslocaNaMesmaProporcaoDoDedo() {
    val cliente = Cliente(id = "14", nome = "Joana", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val fotos =
      listOf(
        Foto(id = "f1", clienteId = cliente.id, uriLocal = "/f1.jpg"),
        Foto(id = "f2", clienteId = cliente.id, uriLocal = "/f2.jpg"),
      )
    val repository = FakeDetailRepository(ClienteComFotos(cliente = cliente, fotos = fotos))
    val viewModel = PhotoViewerViewModel(repository, cliente.id, initialIndex = 0)
    composeTestRule.setContent {
      ElisdbTheme {
        PhotoViewerScreen(viewModel = viewModel, onBack = {})
      }
    }
    composeTestRule.waitForIdle()

    val foto = composeTestRule.onNodeWithContentDescription("Foto")
    val localCenter = foto.fetchSemanticsNode().boundsInRoot.center

    foto.performTouchInput {
      pinch(
        start0 = localCenter - Offset(50f, 0f),
        end0 = localCenter - Offset(200f, 0f),
        start1 = localCenter + Offset(50f, 0f),
        end1 = localCenter + Offset(200f, 0f),
      )
    }
    composeTestRule.waitForIdle()

    val scale = foto.fetchSemanticsNode().config[PhotoPanScale]
    assertTrue("A pinça deveria ampliar a foto", scale > 1f)

    val offsetBefore = foto.fetchSemanticsNode().config[PhotoPanOffset]

    foto.performTouchInput {
      down(localCenter)
      moveBy(Offset(150f, 0f))
      up()
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("1 / 2").assertExists()

    val offsetAfter = foto.fetchSemanticsNode().config[PhotoPanOffset]
    val deslocamento = offsetAfter.x - offsetBefore.x
    assertEquals(
      "O pan deveria deslocar a foto na mesma proporção do dedo (scale=$scale offset=$offsetBefore->$offsetAfter)",
      150f,
      deslocamento,
      30f,
    )
  }

  private fun setDetailContent(cliente: Cliente) {
    val repository = FakeDetailRepository(ClienteComFotos(cliente = cliente, fotos = emptyList()))
    val viewModel = ClientDetailViewModel(repository, cliente.id)
    composeTestRule.setContent {
      ElisdbTheme {
        ClientDetailScreen(
          viewModel = viewModel,
          onBack = {},
          onEdit = {},
          onViewLog = {},
          onDeleted = {},
          onPhotoClick = {},
          onOpenFullscreenMap = {},
        )
      }
    }
  }
}

private class FakeDetailRepository(
  private val data: ClienteComFotos,
) : ClienteRepository {
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

  override val syncFault: Flow<com.example.elisdb.sync.SyncFault?> = flowOf(null)

  override suspend fun syncNow(): SyncStatus = SyncStatus.IDLE
}
