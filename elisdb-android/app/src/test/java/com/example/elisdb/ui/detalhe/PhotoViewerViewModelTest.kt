package com.example.elisdb.ui.detalhe

import android.net.Uri
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import java.io.File
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoViewerViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun carregaFotosEIndice() = runTest(dispatcher) {
    val fotos =
      listOf(
        Foto(id = "1", clienteId = "1", uriLocal = "/a.jpg"),
        Foto(id = "2", clienteId = "1", uriLocal = "/b.jpg"),
      )
    val repo = FotoClientRepository(fotos = fotos)
    val viewModel = PhotoViewerViewModel(repo, "1", initialIndex = 1)
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(2, viewModel.uiState.value.fotos.size)
    assertEquals(1, viewModel.uiState.value.indice)
  }

  @Test
  fun indiceInicialForaDosLimitesFicaNoLimite() = runTest(dispatcher) {
    val fotos = listOf(Foto(id = "1", clienteId = "1", uriLocal = "/a.jpg"))
    val repo = FotoClientRepository(fotos = fotos)
    val viewModel = PhotoViewerViewModel(repo, "1", initialIndex = 10)
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(0, viewModel.uiState.value.indice)
  }

  @Test
  fun semFotosIndiceZero() = runTest(dispatcher) {
    val repo = FotoClientRepository(fotos = emptyList())
    val viewModel = PhotoViewerViewModel(repo, "1", initialIndex = 0)
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(0, viewModel.uiState.value.indice)
    assertEquals(0, viewModel.uiState.value.fotos.size)
  }

  @Test
  fun setIndiceDentroDosLimites() = runTest(dispatcher) {
    val fotos =
      listOf(
        Foto(id = "1", clienteId = "1", uriLocal = "/a.jpg"),
        Foto(id = "2", clienteId = "1", uriLocal = "/b.jpg"),
      )
    val repo = FotoClientRepository(fotos = fotos)
    val viewModel = PhotoViewerViewModel(repo, "1", initialIndex = 0)
    dispatcher.scheduler.advanceUntilIdle()
    viewModel.setIndice(1)
    assertEquals(1, viewModel.uiState.value.indice)
    viewModel.setIndice(5)
    assertEquals(1, viewModel.uiState.value.indice)
    viewModel.setIndice(-3)
    assertEquals(0, viewModel.uiState.value.indice)
  }
}

private class FotoClientRepository(private val fotos: List<Foto>) : ClienteRepository {
  override val clientes: Flow<List<Cliente>> = flowOf(emptyList())

  override val clientesComFotos: Flow<List<ClienteComFotos>> = flowOf(emptyList())

  override fun observeCliente(id: String): Flow<ClienteComFotos?> =
    flowOf(ClienteComFotos(cliente = Cliente(id = "1", nome = "X", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0), fotos = fotos))

  override suspend fun getCliente(id: String): ClienteComFotos? =
    ClienteComFotos(cliente = Cliente(id = "1", nome = "X", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0), fotos = fotos)

  override suspend fun stagePhoto(source: Uri): Foto = error("Not used in this test")

  override fun createPhotoFile(): File = error("Not used in this test")

  override suspend fun deletePhotoFile(foto: Foto) = Unit

  override suspend fun save(draft: ClienteDraft): String = error("Not used in this test")

  override suspend fun deleteCliente(id: String) = Unit

  override val syncStatus = flowOf(SyncStatus.IDLE)

  override val syncFault = flowOf<SyncFault?>(null)

  override suspend fun syncNow(): SyncStatus = SyncStatus.IDLE
}
