package com.example.elisdb.ui.form

import android.net.Uri
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRemovidoException
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import com.example.elisdb.location.Localizacao
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
class ClientFormViewModelTest {
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
  fun uiState_iniciaNovoVazio() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    val state = viewModel.uiState.value
    assertTrue(state.isNew)
    assertNull(state.clienteId)
    assertEquals("", state.nome)
    assertEquals("", state.contrato)
    assertEquals(emptyList<String>(), state.tags)
    assertFalse(state.tagInputVisible)
    assertNull(state.latitude)
  }

  @Test
  fun onNomeChange_escreverMinusculasFicaEmMaiusculasNoEstado() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    viewModel.onNomeChange("maRiA de sousa")
    assertEquals("MARIA DE SOUSA", viewModel.uiState.value.nome)
    viewModel.onNomeChange("maria")
    assertEquals("MARIA", viewModel.uiState.value.nome)
  }

  @Test
  fun editMode_carregaContratoExistente() = runTest(dispatcher) {
    val cliente = Cliente(id = "8", nome = "Maria", contrato = "CT-123", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val viewModel =
      ClientFormViewModel(
        FakeClientRepository(existing = ClienteComFotos(cliente = cliente, fotos = emptyList())),
        { null },
        "8",
      )
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals("CT-123", viewModel.uiState.value.contrato)
  }

  @Test
  fun editMode_carregaTagExistente() = runTest(dispatcher) {
    val cliente = Cliente(id = "9", nome = "Maria", tag = "Premium;GOLD", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val viewModel =
      ClientFormViewModel(
        FakeClientRepository(existing = ClienteComFotos(cliente = cliente, fotos = emptyList())),
        { null },
        "9",
      )
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(listOf("GOLD", "PREMIUM"), viewModel.uiState.value.tags)
  }

  @Test
  fun onTagInputChange_escreverMinusculasFicaEmMaiusculas() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    viewModel.onTagInputChange("premium")
    assertEquals("PREMIUM", viewModel.uiState.value.tagInput)
  }

  @Test
  fun onTagInputChange_limitaAoCemCaracteres() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    viewModel.onTagInputChange("x".repeat(150))
    assertEquals(100, viewModel.uiState.value.tagInput.length)
  }

  @Test
  fun onStartAddTag_mostraCampoDeTexto() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    assertFalse(viewModel.uiState.value.tagInputVisible)
    viewModel.onStartAddTag()
    assertTrue(viewModel.uiState.value.tagInputVisible)
    viewModel.onCancelAddTag()
    assertFalse(viewModel.uiState.value.tagInputVisible)
  }

  @Test
  fun onAddTag_adicionaEmMaiusculasEOrdenado() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    viewModel.onTagInputChange("premium")
    viewModel.onAddTag()
    viewModel.onTagInputChange("gold")
    viewModel.onAddTag()
    assertEquals(listOf("GOLD", "PREMIUM"), viewModel.uiState.value.tags)
    assertEquals("", viewModel.uiState.value.tagInput)
    assertFalse(viewModel.uiState.value.tagInputVisible)
  }

  @Test
  fun onAddTag_ignoraDuplicados() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    viewModel.onTagInputChange("premium")
    viewModel.onAddTag()
    viewModel.onTagInputChange("PREMIUM")
    viewModel.onAddTag()
    assertEquals(listOf("PREMIUM"), viewModel.uiState.value.tags)
  }

  @Test
  fun onRemoveTag_removeDoEstado() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    viewModel.onTagInputChange("premium")
    viewModel.onAddTag()
    viewModel.onTagInputChange("gold")
    viewModel.onAddTag()
    viewModel.onRemoveTag("PREMIUM")
    assertEquals(listOf("GOLD"), viewModel.uiState.value.tags)
  }

  @Test
  fun save_incluiTagNoDraft() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "45")
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onNomeChange("Maria")
    viewModel.onTagInputChange("premium")
    viewModel.onAddTag()
    viewModel.onTagInputChange("gold")
    viewModel.onAddTag()
    viewModel.onLocationChange(1.0, 2.0)
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(listOf("GOLD", "PREMIUM"), repo.savedDraft?.tags)
  }

  @Test
  fun editMode_carregaClienteExistente() = runTest(dispatcher) {
    val cliente = Cliente(id = "7", nome = "Maria", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val viewModel =
      ClientFormViewModel(
        FakeClientRepository(existing = ClienteComFotos(cliente = cliente, fotos = emptyList())),
        { null },
        "7",
      )
    dispatcher.scheduler.advanceUntilIdle()
    val state = viewModel.uiState.value
    assertFalse(state.isNew)
    assertEquals("Maria", state.nome)
    assertEquals(1.0, state.latitude)
    assertEquals(2.0, state.longitude)
  }

  @Test
  fun save_semNomeMarcaErro() = runTest(dispatcher) {
    val repo = FakeClientRepository()
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onLocationChange(1.0, 2.0)
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.uiState.value.nomeError)
    assertFalse(repo.saved)
  }

  @Test
  fun save_semLocalizacaoPersisteClienteNovo() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "99")
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onNomeChange("Maria")
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertTrue(repo.saved)
    assertNull(repo.savedDraft?.latitude)
    assertNull(repo.savedDraft?.longitude)
    assertNull(viewModel.uiState.value.message)
  }

  @Test
  fun save_semLocalizacaoPersisteEdicaoSemCoordenadas() = runTest(dispatcher) {
    val cliente = Cliente(id = "5", nome = "Maria", latitude = 1.0, longitude = 2.0, dataCriacao = 0, updatedAt = 0)
    val repo = FakeClientRepository(existing = ClienteComFotos(cliente = cliente, fotos = emptyList()), savedId = "5")
    val viewModel =
      ClientFormViewModel(
        repo,
        { null },
        "5",
      )
    dispatcher.scheduler.advanceUntilIdle()
    viewModel.onNomeChange("Maria Editada")
    viewModel.onLocationChange(0.0, 0.0)
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertTrue(repo.saved)
    assertEquals(0.0, repo.savedDraft?.latitude)
    assertEquals(0.0, repo.savedDraft?.longitude)
  }

  @Test
  fun save_falhaMostraMensagemEResetaSaving() = runTest(dispatcher) {
    val repo = FakeClientRepository(throwOnSave = true)
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onNomeChange("Maria")
    viewModel.onLocationChange(1.0, 2.0)
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    val state = viewModel.uiState.value
    assertFalse(state.saving)
    assertEquals("Não foi possível guardar o cliente.", state.message)
  }

  @Test
  fun save_clienteRemovidoMostraMensagemEspecificaSemEmitirSaved() = runTest(dispatcher) {
    val repo =
      FakeClientRepository(
        existing = ClienteComFotos(cliente = Cliente(id = "6", nome = "Maria", dataCriacao = 0, updatedAt = 0), fotos = emptyList()),
        saveError = ClienteRemovidoException("6"),
      )
    val viewModel = ClientFormViewModel(repo, { null }, "6")
    dispatcher.scheduler.advanceUntilIdle()
    var savedId: String? = null
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.saved.collect { savedId = it }
    }
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    val state = viewModel.uiState.value
    assertFalse(state.saving)
    assertEquals(
      "Este cliente foi eliminado entretanto. As alterações não foram guardadas.",
      state.message,
    )
    assertNull(savedId)
  }

  @Test
  fun save_semCollectorNaoSuspendeNemBloqueia() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "77")
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onNomeChange("Maria")
    // No collector yet: the save completes instead of suspending on the emit.
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertTrue(repo.saved)
    assertNull(viewModel.uiState.value.message)

    var savedId: String? = null
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.saved.collect { savedId = it }
    }
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals("77", savedId)
  }

  @Test
  fun save_validoPersisteEEmiteId() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "42")
    val viewModel = ClientFormViewModel(repo, { null }, null)
    var savedId: String? = null
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.saved.collect { savedId = it }
    }
    viewModel.onNomeChange("  Maria  ")
    viewModel.onLocationChange(1.0, 2.0)
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals("42", savedId)
    assertEquals("  MARIA  ", repo.savedDraft?.nome)
    assertNull(repo.savedDraft?.id)
  }

  @Test
  fun save_validoIncluiContratoNoDraft() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "43")
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onNomeChange("Maria")
    viewModel.onContratoChange("CT-123")
    viewModel.onLocationChange(1.0, 2.0)
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals("CT-123", repo.savedDraft?.contrato)
  }

  @Test
  fun save_contratoEmBrancoVaiComoVazioNoDraft() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "44")
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onNomeChange("Maria")
    viewModel.onContratoChange("  ")
    viewModel.onLocationChange(1.0, 2.0)
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals("  ", repo.savedDraft?.contrato)
  }

  @Test
  fun useCurrentLocation_atualizaCoordenadas() = runTest(dispatcher) {
    val location = Localizacao(latitude = 10.0, longitude = 20.0)
    val viewModel = ClientFormViewModel(FakeClientRepository(), { location }, null)
    viewModel.useCurrentLocation()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(10.0, viewModel.uiState.value.latitude)
    assertEquals(20.0, viewModel.uiState.value.longitude)
  }

  @Test
  fun useCurrentLocation_semLocalizacaoMostraMensagem() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    viewModel.useCurrentLocation()
    dispatcher.scheduler.advanceUntilIdle()
    assertNull(viewModel.uiState.value.latitude)
    assertEquals("Não foi possível obter a localização atual.", viewModel.uiState.value.message)
  }

  @Test
  fun uiState_novoClienteComecaAtivo() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeClientRepository(), { null }, null)
    assertTrue(viewModel.uiState.value.ativo)
  }

  @Test
  fun save_clienteDesativadoEnviaAtivoFalso() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "45")
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onNomeChange("Maria")
    viewModel.onAtivoChange(false)
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertFalse(repo.savedDraft?.ativo ?: true)
  }

  @Test
  fun save_validoDisparaSincronizacao() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "46")
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onNomeChange("Maria")
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(1, repo.syncCalls)
  }

  @Test
  fun save_falhadoNaoDisparaSincronizacao() = runTest(dispatcher) {
    val repo = FakeClientRepository(saveError = IllegalStateException("falha"))
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.onNomeChange("Maria")
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(0, repo.syncCalls)
  }

  @Test
  fun save_semNomeNaoDisparaSincronizacao() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "47")
    val viewModel = ClientFormViewModel(repo, { null }, null)
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(0, repo.syncCalls)
  }

  @Test
  fun falhaDeSyncNaoImpedeASaidaDoFormulario() = runTest(dispatcher) {
    val repo = FakeClientRepository(savedId = "48", syncError = IllegalStateException("offline"))
    val viewModel = ClientFormViewModel(repo, { null }, null)
    var savedId: String? = null
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.saved.collect { savedId = it }
    }
    viewModel.onNomeChange("Maria")
    viewModel.save()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals("48", savedId)
    assertEquals(1, repo.syncCalls)
  }
}

private class FakeClientRepository(
  private val existing: ClienteComFotos? = null,
  private val savedId: String = "",
  private val throwOnSave: Boolean = false,
  private val saveError: Throwable? = null,
  private val syncError: Throwable? = null,
) : ClienteRepository {
  var saved = false
    private set
  var savedDraft: ClienteDraft? = null
    private set
  var syncCalls = 0
    private set

  override val clientes: Flow<List<Cliente>> = flowOf(emptyList())

  override val clientesComFotos: Flow<List<ClienteComFotos>> = flowOf(emptyList())

  override fun observeCliente(id: String): Flow<ClienteComFotos?> = flowOf(existing)

  override suspend fun getCliente(id: String): ClienteComFotos? = existing

  override suspend fun stagePhoto(source: Uri): Foto = error("Not used in this test")

  override fun createPhotoFile(): File = error("Not used in this test")

  override suspend fun deletePhotoFile(foto: Foto) = Unit

  override suspend fun save(draft: ClienteDraft): String {
    saveError?.let { throw it }
    if (throwOnSave) error("Falha simulada")
    saved = true
    savedDraft = draft
    return savedId
  }

  override suspend fun deleteCliente(id: String) = Unit

  override val syncStatus = flowOf(SyncStatus.IDLE)

  override val syncFault = flowOf<SyncFault?>(null)

  override suspend fun syncNow(): SyncStatus {
    syncCalls++
    syncError?.let { throw it }
    return SyncStatus.IDLE
  }
}
