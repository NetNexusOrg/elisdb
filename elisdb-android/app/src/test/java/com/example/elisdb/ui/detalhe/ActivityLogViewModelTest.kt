package com.example.elisdb.ui.detalhe

import com.example.elisdb.remote.ActivityPageDto
import com.example.elisdb.remote.AuditDetailsDto
import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.remote.AuditPhotosDto
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityLogViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun entry(
    action: String = "edit",
    ts: Long = 100L,
    clienteId: String? = "c1",
    clienteNome: String? = "ANA",
    addedPhotos: List<String> = emptyList(),
  ) =
    AuditEntryDto(
      action = action,
      username = "Maria",
      keyId = "k1",
      ts = ts,
      details =
        if (addedPhotos.isEmpty()) {
          null
        } else {
          AuditDetailsDto(photos = AuditPhotosDto(added = addedPhotos))
        },
      clienteId = clienteId,
      clienteNome = clienteNome,
    )

  @Test
  fun carregaAPrimeiraPaginaComMiniaturasEClientesNavegaveis() = runTest(dispatcher) {
    var requested: Pair<Int, Int?>? = null
    val viewModel =
      ActivityLogViewModel(
        fetchPage = { limit, beforeId ->
          requested = limit to beforeId
          ActivityPageDto(listOf(entry(addedPhotos = listOf("f1"))), nextBeforeId = 7)
        },
        photosByIds = { ids -> if (ids == listOf("f1")) mapOf("f1" to "/tmp/f1.webp") else emptyMap() },
        findExistingClienteIds = { ids -> if (ids == listOf("c1")) setOf("c1") else emptySet() },
      )

    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(50 to null, requested)
    assertFalse(state.isLoading)
    assertFalse(state.error)
    assertEquals(1, state.entries.size)
    assertTrue(state.hasMore)
    assertEquals(mapOf("f1" to "/tmp/f1.webp"), state.photoUris)
    assertEquals(setOf("c1"), state.existingClienteIds)
  }

  @Test
  fun carregaMaisPaginasSemRepetirAsAnteriores() = runTest(dispatcher) {
    val first = entry(ts = 200L)
    val second = entry(ts = 100L, clienteId = "c2", clienteNome = "BEA")
    val cursors = mutableListOf<Int?>()
    val viewModel =
      ActivityLogViewModel(
        fetchPage = { _, beforeId ->
          cursors += beforeId
          if (beforeId == null) {
            ActivityPageDto(listOf(first), nextBeforeId = 10)
          } else {
            ActivityPageDto(listOf(second), nextBeforeId = null)
          }
        },
      )

    dispatcher.scheduler.advanceUntilIdle()
    viewModel.loadMore()
    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(listOf(null, 10), cursors)
    assertEquals(listOf(first, second), state.entries)
    assertFalse(state.hasMore)
    assertFalse(state.isLoadingMore)
  }

  @Test
  fun naoCarregaMaisDepoisDaUltimaPagina() = runTest(dispatcher) {
    var calls = 0
    val viewModel =
      ActivityLogViewModel(
        fetchPage = { _, _ ->
          calls++
          ActivityPageDto(listOf(entry()), nextBeforeId = null)
        }
      )

    dispatcher.scheduler.advanceUntilIdle()
    viewModel.loadMore()
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, calls)
  }

  @Test
  fun erroMostraErroERetryRecarrega() = runTest(dispatcher) {
    var calls = 0
    val viewModel =
      ActivityLogViewModel(
        fetchPage = { _, _ ->
          calls++
          if (calls == 1) error("falha simulada")
          ActivityPageDto(listOf(entry()), nextBeforeId = null)
        }
      )

    dispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.uiState.value.error)
    assertFalse(viewModel.uiState.value.isLoading)

    viewModel.retry()
    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.error)
    assertEquals(1, state.entries.size)
    assertEquals(2, calls)
  }

  @Test
  fun listaVaziaNaoFicaEmCarregamento() = runTest(dispatcher) {
    val viewModel = ActivityLogViewModel(fetchPage = { _, _ -> ActivityPageDto() })

    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertFalse(state.error)
    assertTrue(state.entries.isEmpty())
    assertFalse(state.hasMore)
  }

  @Test
  fun falhaAoCarregarMaisMantemAsEntradas() = runTest(dispatcher) {
    var calls = 0
    val viewModel =
      ActivityLogViewModel(
        fetchPage = { _, _ ->
          calls++
          if (calls == 1) ActivityPageDto(listOf(entry()), nextBeforeId = 9)
          else error("falha simulada")
        }
      )

    dispatcher.scheduler.advanceUntilIdle()
    viewModel.loadMore()
    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.entries.size)
    assertFalse(state.isLoadingMore)
    assertFalse(state.error)
    assertTrue(state.hasMore)
  }
}
