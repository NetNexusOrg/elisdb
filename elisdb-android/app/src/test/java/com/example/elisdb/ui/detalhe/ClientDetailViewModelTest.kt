package com.example.elisdb.ui.detalhe

import android.net.Uri
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import java.io.File
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDetailViewModelTest {
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
  fun carregaCliente() = runTest(dispatcher) {
    val cliente = Cliente(id = "5", nome = "Maria", contrato = "CT-123", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val repo = FakeClientRepository(data = ClienteComFotos(cliente = cliente, fotos = emptyList()))
    val viewModel = ClientDetailViewModel(repo, "5")
    dispatcher.scheduler.advanceUntilIdle()
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Maria", state.cliente?.cliente?.nome)
    assertEquals("CT-123", state.cliente?.cliente?.contrato)
  }

  @Test
  fun deleteClienteChamaRepoEEmite() = runTest(dispatcher) {
    val cliente = Cliente(id = "5", nome = "Maria", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val repo = FakeClientRepository(data = ClienteComFotos(cliente = cliente, fotos = emptyList()))
    val viewModel = ClientDetailViewModel(repo, "5")
    dispatcher.scheduler.advanceUntilIdle()
    var deleted = false
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.deleted.collect { deleted = true }
    }
    viewModel.showDeleteConfirmation()
    assertTrue(viewModel.uiState.value.showDeleteConfirmation)
    viewModel.deleteCliente()
    dispatcher.scheduler.advanceUntilIdle()
    assertTrue(repo.deletedId == "5")
    assertTrue(deleted)
    assertFalse(viewModel.uiState.value.showDeleteConfirmation)
  }

  @Test
  fun notificacaoNavegacaoIndisponivel() = runTest(dispatcher) {
    val repo = FakeClientRepository(data = null)
    val viewModel = ClientDetailViewModel(repo, "5")
    dispatcher.scheduler.advanceUntilIdle()
    assertNull(viewModel.uiState.value.navigateMessage)
    viewModel.notifyNavigationUnavailable()
    assertEquals("Não foi encontrada uma aplicação de navegação.", viewModel.uiState.value.navigateMessage)
    viewModel.onNavigateMessageShown()
    assertNull(viewModel.uiState.value.navigateMessage)
  }

  @Test
  fun deleteClienteFalhaMostraMensagem() = runTest(dispatcher) {
    val cliente = Cliente(id = "5", nome = "Maria", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val repo = FakeClientRepository(data = ClienteComFotos(cliente = cliente, fotos = emptyList()), throwOnDelete = true)
    val viewModel = ClientDetailViewModel(repo, "5")
    dispatcher.scheduler.advanceUntilIdle()
    viewModel.showDeleteConfirmation()
    viewModel.deleteCliente()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals("Não foi possível apagar o cliente.", viewModel.uiState.value.message)
  }

  @Test
  fun openLogCarregaHistorico() = runTest(dispatcher) {
    val cliente = Cliente(id = "5", nome = "Maria", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val repo = FakeClientRepository(data = ClienteComFotos(cliente = cliente, fotos = emptyList()))
    val entries =
      listOf(
        AuditEntryDto(action = "create", username = "Maria", keyId = "k1", ts = 100L),
        AuditEntryDto(action = "edit", username = "Joao", keyId = "k2", ts = 200L),
      )
    val viewModel = ClientDetailViewModel(repo, "5", fetchHistory = { id -> entries })
    dispatcher.scheduler.advanceUntilIdle()
    viewModel.openLog()
    dispatcher.scheduler.advanceUntilIdle()
    val state = viewModel.uiState.value
    assertTrue(state.showLog)
    assertFalse(state.logLoading)
    assertFalse(state.logError)
    assertEquals(2, state.logEntries.size)
    assertEquals("create", state.logEntries[0].action)
  }

  @Test
  fun openLogFalhaENaoBloqueia() = runTest(dispatcher) {
    val cliente = Cliente(id = "5", nome = "Maria", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val repo = FakeClientRepository(data = ClienteComFotos(cliente = cliente, fotos = emptyList()))
    val viewModel =
      ClientDetailViewModel(repo, "5", fetchHistory = { id -> error("falha simulada") })
    dispatcher.scheduler.advanceUntilIdle()
    viewModel.openLog()
    dispatcher.scheduler.advanceUntilIdle()
    val state = viewModel.uiState.value
    assertTrue(state.showLog)
    assertFalse(state.logLoading)
    assertTrue(state.logError)
    assertTrue(state.logEntries.isEmpty())
    viewModel.dismissLog()
    assertFalse(viewModel.uiState.value.showLog)
  }

  @Test
  fun hasPending_refleteClientePendente() = runTest(dispatcher) {
    val cliente = Cliente(id = "5", nome = "Maria", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0, pending = true)
    val repo = FakeClientRepository(data = ClienteComFotos(cliente = cliente, fotos = emptyList()))
    val viewModel = ClientDetailViewModel(repo, "5")
    dispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.uiState.value.hasPending)
  }

  @Test
  fun hasPending_falsoParaClienteSincronizado() = runTest(dispatcher) {
    val cliente = Cliente(id = "5", nome = "Maria", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0, pending = false)
    val repo = FakeClientRepository(data = ClienteComFotos(cliente = cliente, fotos = emptyList()))
    val viewModel = ClientDetailViewModel(repo, "5")
    dispatcher.scheduler.advanceUntilIdle()
    assertFalse(viewModel.uiState.value.hasPending)
  }

  @Test
  fun clienteInexistente_terminaLoadingENaoEncontrado() = runTest(dispatcher) {
    val repo = FakeClientRepository(data = null)
    val viewModel = ClientDetailViewModel(repo, "missing")
    dispatcher.scheduler.advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.cliente)
  }
}

private class FakeClientRepository(
  private val data: ClienteComFotos?,
  private val throwOnDelete: Boolean = false,
) : ClienteRepository {
  var deletedId: String? = null

  override val clientes: Flow<List<Cliente>> = flowOf(emptyList())

  override val clientesComFotos: Flow<List<ClienteComFotos>> = flowOf(emptyList())

  override fun observeCliente(id: String): Flow<ClienteComFotos?> = flowOf(data)

  override suspend fun getCliente(id: String): ClienteComFotos? = data

  override suspend fun stagePhoto(source: Uri): Foto = error("Not used in this test")

  override fun createPhotoFile(): File = error("Not used in this test")

  override suspend fun deletePhotoFile(foto: Foto) = Unit

  override suspend fun save(draft: ClienteDraft): String = error("Not used in this test")

  override suspend fun deleteCliente(id: String) {
    if (throwOnDelete) error("Falha simulada")
    deletedId = id
  }

  override val syncStatus = flowOf(SyncStatus.IDLE)

  override val syncFault = flowOf<SyncFault?>(null)

  override suspend fun syncNow(): SyncStatus = SyncStatus.IDLE
}
