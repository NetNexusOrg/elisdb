package com.example.elisdb.ui.cliente

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientListViewModelTest {
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
  fun uiState_iniciaVazio() = runTest(dispatcher) {
    val viewModel = ClientListViewModel(FakeClientRepository())
    val state = viewModel.uiState.first { it is ClientListUiState.Success }
    assertEquals(listOf<ClientListItem>(), (state as ClientListUiState.Success).clientes)
  }

  @Test
  fun uiState_mostraClienteEPrimeiraFoto() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente = Cliente(id = "1", nome = "Maria", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos =
                  listOf(
                    Foto(id = "1", clienteId = "1", uriLocal = "/path/a.jpg"),
                    Foto(id = "2", clienteId = "1", uriLocal = "/path/b.jpg"),
                  ),
              )
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    val state = viewModel.uiState.first { it is ClientListUiState.Success }
    val item = (state as ClientListUiState.Success).clientes.single()
    assertEquals("Maria", item.nome)
    assertEquals("/path/a.jpg", item.primeiraFotoUri)
  }

  @Test
  fun uiState_semFotosMostraNull() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente = Cliente(id = "1", nome = "João", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              )
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    val state = viewModel.uiState.first { it is ClientListUiState.Success }
    assertEquals(null, (state as ClientListUiState.Success).clientes.single().primeiraFotoUri)
  }

  @Test
  fun uiState_mapeiaTagDoCliente() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente = Cliente(id = "1", nome = "Maria", tag = "Premium", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              )
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    val state = viewModel.uiState.first { it is ClientListUiState.Success }
    assertEquals(listOf("PREMIUM"), (state as ClientListUiState.Success).clientes.single().tags)
  }

  @Test
  fun uiState_queryFiltraClientes() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente = Cliente(id = "1", nome = "Maria", contrato = "CT-100", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
              ClienteComFotos(
                cliente = Cliente(id = "2", nome = "João", contrato = "CT-200", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    viewModel.uiState.first { it is ClientListUiState.Success }

    viewModel.onQueryChange("ct-1")
    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as ClientListUiState.Success
    assertEquals(listOf("Maria"), state.clientes.map { it.nome })
    assertEquals("ct-1", state.query)
  }

  @Test
  fun uiState_queryVaziaMostraTodos() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente = Cliente(id = "1", nome = "Maria", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
              ClienteComFotos(
                cliente = Cliente(id = "2", nome = "João", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    viewModel.uiState.first { it is ClientListUiState.Success }

    viewModel.onQueryChange("")
    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as ClientListUiState.Success
    assertEquals(listOf("João", "Maria"), state.clientes.map { it.nome })
  }

  @Test
  fun uiState_filtroDeEstadoMostraSoOEstadoEscolhido() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente = Cliente(id = "1", nome = "Maria", ativo = true, latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
              ClienteComFotos(
                cliente = Cliente(id = "2", nome = "João", ativo = false, latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    viewModel.uiState.first { it is ClientListUiState.Success }

    viewModel.onFiltroChange(ClientStatusFilter.INACTIVE)
    var state =
      viewModel.uiState.first { it is ClientListUiState.Success && it.filtro == ClientStatusFilter.INACTIVE }
        as ClientListUiState.Success
    assertEquals(listOf("João"), state.clientes.map { it.nome })

    viewModel.onFiltroChange(ClientStatusFilter.ACTIVE)
    state =
      viewModel.uiState.first { it is ClientListUiState.Success && it.filtro == ClientStatusFilter.ACTIVE }
        as ClientListUiState.Success
    assertEquals(listOf("Maria"), state.clientes.map { it.nome })

    viewModel.onFiltroChange(ClientStatusFilter.ALL)
    state =
      viewModel.uiState.first { it is ClientListUiState.Success && it.filtro == ClientStatusFilter.ALL }
        as ClientListUiState.Success
    assertEquals(listOf("João", "Maria"), state.clientes.map { it.nome })
  }

  @Test
  fun uiState_tagsDisponiveisSaoDistintasEmMaiusculasEOrdenadas() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente = Cliente(id = "1", nome = "Maria", tag = "premium", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
              ClienteComFotos(
                cliente = Cliente(id = "2", nome = "João", tag = "Premium", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
              ClienteComFotos(
                cliente = Cliente(id = "3", nome = "Ana", tag = "gold", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
              ClienteComFotos(
                cliente = Cliente(id = "4", nome = "Rui", tag = null, latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    val state = viewModel.uiState.first { it is ClientListUiState.Success } as ClientListUiState.Success
    assertEquals(listOf("GOLD", "PREMIUM"), state.tagsDisponiveis)
  }

  @Test
  fun uiState_filtroDeTagMostraSoClientesComATag() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente = Cliente(id = "1", nome = "Maria", tag = "premium", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
              ClienteComFotos(
                cliente = Cliente(id = "2", nome = "João", tag = "gold", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              ),
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    viewModel.uiState.first { it is ClientListUiState.Success }

    viewModel.onTagFiltroChange("PREMIUM")
    var state =
      viewModel.uiState.first { it is ClientListUiState.Success && it.tagFiltro == "PREMIUM" }
        as ClientListUiState.Success
    assertEquals(listOf("Maria"), state.clientes.map { it.nome })

    viewModel.onTagFiltroChange(null)
    state =
      viewModel.uiState.first { it is ClientListUiState.Success && it.tagFiltro == null }
        as ClientListUiState.Success
    assertEquals(listOf("João", "Maria"), state.clientes.map { it.nome })
  }

  @Test
  fun uiState_ordemRecenteMostraMaisNovosPrimeiro() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente =
                  Cliente(id = "1", nome = "Ana", dataCriacao = 100, updatedAt = 500),
                fotos = emptyList(),
              ),
              ClienteComFotos(
                cliente =
                  Cliente(id = "2", nome = "Zé", dataCriacao = 300, updatedAt = 300),
                fotos = emptyList(),
              ),
              ClienteComFotos(
                cliente =
                  Cliente(id = "3", nome = "Bruno", dataCriacao = 200, updatedAt = 900),
                fotos = emptyList(),
              ),
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    viewModel.uiState.first { it is ClientListUiState.Success }

    var state = viewModel.uiState.value as ClientListUiState.Success
    assertEquals(listOf("Ana", "Bruno", "Zé"), state.clientes.map { it.nome })

    viewModel.onOrdemChange(ClientSortOrder.RECENT)
    state =
      viewModel.uiState.first { it is ClientListUiState.Success && it.ordem == ClientSortOrder.RECENT }
        as ClientListUiState.Success
    assertEquals(listOf("Zé", "Bruno", "Ana"), state.clientes.map { it.nome })

    viewModel.onOrdemChange(ClientSortOrder.UPDATED)
    state =
      viewModel.uiState.first { it is ClientListUiState.Success && it.ordem == ClientSortOrder.UPDATED }
        as ClientListUiState.Success
    assertEquals(listOf("Bruno", "Ana", "Zé"), state.clientes.map { it.nome })

    viewModel.onOrdemChange(ClientSortOrder.NAME)
    state =
      viewModel.uiState.first { it is ClientListUiState.Success && it.ordem == ClientSortOrder.NAME }
        as ClientListUiState.Success
    assertEquals(listOf("Ana", "Bruno", "Zé"), state.clientes.map { it.nome })
  }

  @Test
  fun uiState_querySemCorrespondenciaDevolveVazio() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        clientesComFotosFlow =
          flowOf(
            listOf(
              ClienteComFotos(
                cliente = Cliente(id = "1", nome = "Maria", latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
                fotos = emptyList(),
              )
            )
          )
      )
    val viewModel = ClientListViewModel(repo)
    viewModel.uiState.first { it is ClientListUiState.Success }

    viewModel.onQueryChange("xyz")
    dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as ClientListUiState.Success
    assertEquals(emptyList<ClientListItem>(), state.clientes)
  }

  @Test
  fun uiState_inicialUsaPreferenciasGuardadas() = runTest(dispatcher) {
    val preferences =
      FakeClientViewPreferences(
        listFilter = ClientStatusFilter.INACTIVE,
        sortOrder = ClientSortOrder.RECENT,
      )
    val viewModel = ClientListViewModel(FakeClientRepository(), preferences)

    val state = viewModel.uiState.first { it is ClientListUiState.Success } as ClientListUiState.Success

    assertEquals(ClientStatusFilter.INACTIVE, state.filtro)
    assertEquals(ClientSortOrder.RECENT, state.ordem)
  }

  @Test
  fun alteracoesDeFiltroEOrdemSaoGuardadas() = runTest(dispatcher) {
    val preferences = FakeClientViewPreferences()
    val viewModel = ClientListViewModel(FakeClientRepository(), preferences)
    viewModel.uiState.first { it is ClientListUiState.Success }

    viewModel.onFiltroChange(ClientStatusFilter.INACTIVE)
    viewModel.onOrdemChange(ClientSortOrder.UPDATED)

    assertEquals(ClientStatusFilter.INACTIVE, preferences.listFilter)
    assertEquals(ClientSortOrder.UPDATED, preferences.sortOrder)
  }
}

private class FakeClientViewPreferences(
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

private class FakeClientRepository(
  private val clientesComFotosFlow: Flow<List<ClienteComFotos>> = flowOf(emptyList()),
) : ClienteRepository {
  override val clientes: Flow<List<Cliente>> = flowOf(emptyList())

  override val clientesComFotos: Flow<List<ClienteComFotos>> = clientesComFotosFlow

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
