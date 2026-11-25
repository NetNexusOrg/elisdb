package com.example.elisdb.ui.catalogo

import com.example.elisdb.data.CatalogoNomeDuplicadoException
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.CatalogoItem
import com.example.elisdb.data.local.CategoriaCatalogo
import com.example.elisdb.data.local.ClienteServicoComItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogoViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() = Dispatchers.setMain(dispatcher)

  @After
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun listaOrdenaPorCategoriaENomeIgnorandoAcentos() = runTest {
    val repo =
      FakeCatalogoRepository(
        listOf(
          item("1", CategoriaCatalogo.TRATAMENTOS, "Zeta"),
          item("2", CategoriaCatalogo.ROEDORES, "Ábaco"),
          item("3", CategoriaCatalogo.ROEDORES, "Aarmadilha"),
        )
      )
    val viewModel = CatalogoViewModel(repo)
    var latest = CatalogoUiState()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect { latest = it }
    }
    advanceUntilIdle()

    assertEquals(listOf("3", "2", "1"), latest.itens.map { it.id })
    assertEquals(false, latest.isLoading)
  }

  @Test
  fun nomeDuplicadoMostraMensagem() = runTest {
    val repo = FakeCatalogoRepository(emptyList(), failWithDuplicate = true)
    val viewModel = CatalogoViewModel(repo)

    viewModel.save(null, "Ratoeira", CategoriaCatalogo.ROEDORES) {}
    advanceUntilIdle()

    assertEquals(
      "Já existe um modelo com esse nome nesta categoria.",
      viewModel.message.value,
    )
  }

  @Test
  fun guardarDisparaSincronizacaoEMostraConfirmacao() = runTest {
    val repo = FakeCatalogoRepository(emptyList())
    val viewModel = CatalogoViewModel(repo)

    viewModel.guardar()
    advanceUntilIdle()

    assertEquals(1, repo.syncCalls)
    assertEquals("Catálogo guardado.", viewModel.message.value)
  }

  @Test
  fun guardarComItensPendentesAvisaQueFaltaSincronizar() = runTest {
    val repo = FakeCatalogoRepository(listOf(item("1", CategoriaCatalogo.ROEDORES, "Beta").copy(pending = true)))
    val viewModel = CatalogoViewModel(repo)

    viewModel.guardar()
    advanceUntilIdle()

    assertEquals(1, repo.syncCalls)
    assertEquals("Catálogo guardado, mas há itens por sincronizar.", viewModel.message.value)
  }

  private fun item(id: String, categoria: CategoriaCatalogo, nome: String) =
    CatalogoItem(
      id = id,
      categoria = categoria.slug,
      nome = nome,
      nomeNormalizado = com.example.elisdb.data.normalizarNomeCatalogo(nome),
    )
}

private class FakeCatalogoRepository(
  initial: List<CatalogoItem>,
  private val failWithDuplicate: Boolean = false,
) : ClienteRepository {
  val items = MutableStateFlow(initial)
  var syncCalls = 0

  override val catalogo: Flow<List<CatalogoItem>> = items
  override val clientes: Flow<List<com.example.elisdb.data.local.Cliente>> = flowOf(emptyList())
  override val clientesComFotos: Flow<List<com.example.elisdb.data.local.ClienteComFotos>> =
    flowOf(emptyList())

  override fun observeCliente(
    id: String
  ): Flow<com.example.elisdb.data.local.ClienteComFotos?> = flowOf(null)

  override suspend fun getCliente(id: String) = null

  override suspend fun stagePhoto(source: android.net.Uri) = error("not used")

  override fun createPhotoFile(): java.io.File = error("not used")

  override suspend fun deletePhotoFile(foto: com.example.elisdb.data.local.Foto) = Unit

  override suspend fun save(draft: com.example.elisdb.data.ClienteDraft): String = "1"

  override suspend fun deleteCliente(id: String) = Unit

  override val syncStatus: Flow<com.example.elisdb.sync.SyncStatus> =
    flowOf(com.example.elisdb.sync.SyncStatus.IDLE)

  override val syncFault: Flow<com.example.elisdb.sync.SyncFault?> = flowOf(null)

  override suspend fun syncNow(): com.example.elisdb.sync.SyncStatus {
    syncCalls++
    return com.example.elisdb.sync.SyncStatus.SUCCESS
  }

  override fun observeServicos(clienteId: String): Flow<List<ClienteServicoComItem>> =
    flowOf(emptyList())

  override suspend fun getCatalogo(): List<CatalogoItem> = items.value

  override suspend fun getServicos(clienteId: String): List<ClienteServicoComItem> =
    emptyList()

  override suspend fun saveCatalogoItem(
    id: String?,
    nome: String,
    categoria: CategoriaCatalogo,
  ): String {
    if (failWithDuplicate) throw CatalogoNomeDuplicadoException()
    return id ?: "new"
  }

  override suspend fun setCatalogoAtivo(id: String, ativo: Boolean) = Unit
}
