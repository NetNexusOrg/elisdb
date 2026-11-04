package com.example.elisdb.ui.cliente

import android.net.Uri
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import com.example.elisdb.location.Localizacao
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientMapViewModelTest {
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
  fun uiState_mostraApenasClientesComCoordenadasValidas() = runTest(dispatcher) {
    val viewModel =
      ClientMapViewModel(
        FakeMapRepository(
          flowOf(
            listOf(
              cliente("1", "Maria", latitude = 38.7, longitude = -9.1),
              cliente("2", "João", latitude = null, longitude = null),
              cliente("3", "Rui", latitude = 100.0, longitude = -9.1),
              cliente("4", "Ana", latitude = 38.7, longitude = 200.0),
            )
          )
        )
      )

    val state = viewModel.uiState.first { it is ClientMapUiState.Success } as ClientMapUiState.Success

    assertEquals(ClientMapFilter.ACTIVE, viewModel.filtro.value)
    assertEquals(listOf("Maria"), state.clientes.map { it.nome })
    assertEquals(3, state.semLocalizacao)
    assertEquals(4, state.totalClientes)
  }

  @Test
  fun uiState_ordenaPorNomeEIncluiInativoQuandoFiltroTodos() = runTest(dispatcher) {
    val viewModel =
      ClientMapViewModel(
        FakeMapRepository(
          flowOf(
            listOf(
              cliente("1", "Zeca", latitude = 1.0, longitude = 2.0, ativo = false),
              cliente("2", "ana", latitude = 3.0, longitude = 4.0),
            )
          )
        )
      )

    val activeState =
      viewModel.uiState.first { it is ClientMapUiState.Success } as ClientMapUiState.Success
    assertEquals(listOf("ana"), activeState.clientes.map { it.nome })

    viewModel.onFiltroChange(ClientMapFilter.ALL)
    dispatcher.scheduler.advanceUntilIdle()

    val allState = viewModel.uiState.value as ClientMapUiState.Success
    assertEquals(listOf("ana", "Zeca"), allState.clientes.map { it.nome })
    assertFalse(allState.clientes.last().ativo)
  }

  @Test
  fun uiState_filtroInativosMostraApenasInativos() = runTest(dispatcher) {
    val viewModel =
      ClientMapViewModel(
        FakeMapRepository(
          flowOf(
            listOf(
              cliente("1", "Zeca", latitude = 1.0, longitude = 2.0, ativo = false),
              cliente("2", "ana", latitude = 3.0, longitude = 4.0),
            )
          )
        )
      )

    viewModel.uiState.first { it is ClientMapUiState.Success }

    viewModel.onFiltroChange(ClientMapFilter.INACTIVE)
    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as ClientMapUiState.Success
    assertEquals(ClientMapFilter.INACTIVE, viewModel.filtro.value)
    assertEquals(listOf("Zeca"), state.clientes.map { it.nome })
  }

  @Test
  fun uiState_tagsDisponiveisSaoDistintasEmMaiusculasEOrdenadas() = runTest(dispatcher) {
    val viewModel =
      ClientMapViewModel(
        FakeMapRepository(
          flowOf(
            listOf(
              cliente("1", "Maria", latitude = 1.0, longitude = 2.0, tag = "premium"),
              cliente("2", "João", latitude = 3.0, longitude = 4.0, tag = "Premium"),
              cliente("3", "Ana", latitude = 5.0, longitude = 6.0, tag = "gold"),
              cliente("4", "Rui", latitude = 7.0, longitude = 8.0, tag = null),
            )
          )
        )
      )

    val state = viewModel.uiState.first { it is ClientMapUiState.Success } as ClientMapUiState.Success

    assertEquals(listOf("GOLD", "PREMIUM"), state.tagsDisponiveis)
  }

  @Test
  fun uiState_filtroDeTagMostraSoClientesComATag() = runTest(dispatcher) {
    val viewModel =
      ClientMapViewModel(
        FakeMapRepository(
          flowOf(
            listOf(
              cliente("1", "Maria", latitude = 1.0, longitude = 2.0, tag = "premium"),
              cliente("2", "João", latitude = 3.0, longitude = 4.0, tag = "gold"),
            )
          )
        )
      )
    viewModel.uiState.first { it is ClientMapUiState.Success }

    viewModel.onTagFiltroChange("PREMIUM")
    dispatcher.scheduler.advanceUntilIdle()

    var state =
      viewModel.uiState.first { it is ClientMapUiState.Success && it.tagFiltro == "PREMIUM" }
        as ClientMapUiState.Success
    assertEquals(listOf("Maria"), state.clientes.map { it.nome })
    assertEquals("PREMIUM", state.tagFiltro)

    viewModel.onTagFiltroChange(null)
    dispatcher.scheduler.advanceUntilIdle()

    state =
      viewModel.uiState.first { it is ClientMapUiState.Success && it.clientes.size == 2 }
        as ClientMapUiState.Success
    assertEquals(listOf("João", "Maria"), state.clientes.map { it.nome })
    assertEquals(null, state.tagFiltro)
  }

  @Test
  fun uiState_semClientesMostraListaVazia() = runTest(dispatcher) {
    val viewModel = ClientMapViewModel(FakeMapRepository(flowOf(emptyList())))

    val state = viewModel.uiState.first { it is ClientMapUiState.Success } as ClientMapUiState.Success

    assertEquals(emptyList<ClientMapItem>(), state.clientes)
    assertEquals(0, state.semLocalizacao)
    assertEquals(0, state.totalClientes)
  }

  @Test
  fun refreshLocation_guardaLocalizacaoAtual() = runTest(dispatcher) {
    val expected = Localizacao(latitude = 38.7, longitude = -9.1)
    val viewModel =
      ClientMapViewModel(
        FakeMapRepository(flowOf(emptyList())),
        locationProvider = { expected },
      )

    assertEquals(null, viewModel.myLocation.value)
    viewModel.refreshLocation()
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(expected, viewModel.myLocation.value)
  }

  @Test
  fun filtroInicialVemDasPreferencias() = runTest(dispatcher) {
    val preferences = FakeMapClientViewPreferences(mapFilter = ClientMapFilter.INACTIVE)
    val viewModel =
      ClientMapViewModel(
        FakeMapRepository(flowOf(emptyList())),
        viewPreferences = preferences,
      )

    val state = viewModel.uiState.first { it is ClientMapUiState.Success } as ClientMapUiState.Success

    assertEquals(ClientMapFilter.INACTIVE, viewModel.filtro.value)
    assertEquals(emptyList<ClientMapItem>(), state.clientes)
  }

  @Test
  fun alteracaoDeFiltroDoMapaEguardada() = runTest(dispatcher) {
    val preferences = FakeMapClientViewPreferences()
    val viewModel =
      ClientMapViewModel(
        FakeMapRepository(flowOf(emptyList())),
        viewPreferences = preferences,
      )
    viewModel.uiState.first { it is ClientMapUiState.Success }

    viewModel.onFiltroChange(ClientMapFilter.ALL)

    assertEquals(ClientMapFilter.ALL, preferences.mapFilter)
  }
}

private class FakeMapClientViewPreferences(
  var listFilter: ClientStatusFilter = ClientStatusFilter.ALL,
  var sortOrder: ClientSortOrder = ClientSortOrder.NAME,
  var mapFilter: ClientMapFilter = ClientMapFilter.ACTIVE,
) : ClientViewPreferences {
  override fun clientListFilter(): ClientStatusFilter = listFilter

  override fun setClientListFilter(value: ClientStatusFilter) {
    listFilter = value
  }

  override fun clientSortOrder(): ClientSortOrder = sortOrder

  override fun setClientSortOrder(value: ClientSortOrder) {
    sortOrder = value
  }

  override fun clientMapFilter(): ClientMapFilter = mapFilter

  override fun setClientMapFilter(value: ClientMapFilter) {
    mapFilter = value
  }
}

private fun cliente(
  id: String,
  nome: String,
  latitude: Double?,
  longitude: Double?,
  ativo: Boolean = true,
  tag: String? = null,
): Cliente =
  Cliente(
    id = id,
    nome = nome,
    tag = tag,
    latitude = latitude,
    longitude = longitude,
    ativo = ativo,
    dataCriacao = 0,
    updatedAt = 0,
  )

private class FakeMapRepository(
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
