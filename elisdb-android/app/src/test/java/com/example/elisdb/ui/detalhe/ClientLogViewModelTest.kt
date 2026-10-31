package com.example.elisdb.ui.detalhe

import android.net.Uri
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import java.io.File
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
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
class ClientLogViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun cliente(pending: Boolean = false, deletePending: Boolean = false) =
    Cliente(
      id = "5",
      nome = "Maria",
      contrato = "129020",
      latitude = 1.0,
      longitude = 2.0,
      dataCriacao = 0,
      updatedAt = 0,
      pending = pending,
      deletePending = deletePending,
    )

  @Test
  fun carregaHistoricoEExpoeCliente() = runTest(dispatcher) {
    val entries =
      listOf(
        AuditEntryDto(action = "create", username = "Maria", keyId = "k1", ts = 100L),
        AuditEntryDto(action = "edit", username = "Joao", keyId = "k2", ts = 200L),
      )
    val viewModel =
      ClientLogViewModel(
        FakeLogRepository(ClienteComFotos(cliente = cliente(), fotos = emptyList())),
        "5",
      ) { entries }

    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertFalse(state.error)
    assertEquals(2, state.entries.size)
    assertEquals("create", state.entries[0].action)
    assertEquals("Maria", state.clienteNome)
    assertEquals("129020", state.contrato)
    assertFalse(state.hasPending)
  }

  @Test
  fun expoeOsFicheirosLocaisDasFotos() = runTest(dispatcher) {
    val viewModel =
      ClientLogViewModel(
        FakeLogRepository(
          ClienteComFotos(
            cliente = cliente(),
            fotos =
              listOf(
                Foto(id = "f1", clienteId = "5", uriLocal = "/tmp/f1.webp", ordem = 0),
                Foto(id = "f2", clienteId = "5", uriLocal = "/tmp/f2.webp", ordem = 1),
              ),
          )
        ),
        "5",
      )

    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(
      mapOf("f1" to "/tmp/f1.webp", "f2" to "/tmp/f2.webp"),
      viewModel.uiState.value.photoUris,
    )
  }

  @Test
  fun falhaMostraErroERetryRecarrega() = runTest(dispatcher) {
    var calls = 0
    val viewModel =
      ClientLogViewModel(
        FakeLogRepository(ClienteComFotos(cliente = cliente(), fotos = emptyList())),
        "5",
      ) {
        calls++
        if (calls == 1) error("falha simulada")
        listOf(AuditEntryDto(action = "create", username = "Maria", keyId = "k1", ts = 100L))
      }

    dispatcher.scheduler.advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.error)
    assertTrue(viewModel.uiState.value.entries.isEmpty())

    viewModel.retry()
    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.error)
    assertEquals(1, state.entries.size)
    assertEquals(2, calls)
  }

  @Test
  fun clientePendenteMarcaHasPending() = runTest(dispatcher) {
    val viewModel =
      ClientLogViewModel(
        FakeLogRepository(ClienteComFotos(cliente = cliente(pending = true), fotos = emptyList())),
        "5",
      )

    dispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.hasPending)
  }

  @Test
  fun clienteComApagarPendenteMarcaHasPending() = runTest(dispatcher) {
    val viewModel =
      ClientLogViewModel(
        FakeLogRepository(
          ClienteComFotos(cliente = cliente(deletePending = true), fotos = emptyList())
        ),
        "5",
      )

    dispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.hasPending)
  }
}

private class FakeLogRepository(private val data: ClienteComFotos?) : ClienteRepository {
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
