package com.example.elisdb.ui.form

import android.net.Uri
import com.example.elisdb.data.AttachmentRejectedException
import com.example.elisdb.data.AttachmentRejection
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Documento
import com.example.elisdb.data.local.Foto
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Device-map attachments in [ClientFormViewModel]. These need a real `android.net.Uri`,
 * which the JVM unit tests cannot provide, so they live here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentoFormViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private val pdf =
    Documento(
      id = "",
      clienteId = "",
      nome = "planta.pdf",
      mime = "application/pdf",
      uriLocal = "file:/planta.pdf",
    )

  @Test
  fun pickedFileIsListedUnderTheClient() = runTest(dispatcher) {
    val viewModel = ClientFormViewModel(FakeDocumentoRepository(staged = pdf), { null }, null)

    viewModel.onDocumentPicked(Uri.parse("content://docs/planta.pdf"))
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(listOf(pdf), viewModel.uiState.value.documentos)
    assertNull(viewModel.uiState.value.message)
  }

  @Test
  fun unsupportedTypeShowsAMessageAndAddsNothing() = runTest(dispatcher) {
    val repository =
      FakeDocumentoRepository(
        error = AttachmentRejectedException(AttachmentRejection.UNSUPPORTED_TYPE)
      )
    val viewModel = ClientFormViewModel(repository, { null }, null)

    viewModel.onDocumentPicked(Uri.parse("content://docs/contrato.docx"))
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(
      "Tipo de ficheiro não suportado. Adiciona imagens ou PDFs.",
      viewModel.uiState.value.message,
    )
    assertTrue(viewModel.uiState.value.documentos.isEmpty())
  }

  @Test
  fun oversizedFileShowsAMessageAndAddsNothing() = runTest(dispatcher) {
    val repository =
      FakeDocumentoRepository(error = AttachmentRejectedException(AttachmentRejection.TOO_LARGE))
    val viewModel = ClientFormViewModel(repository, { null }, null)

    viewModel.onDocumentPicked(Uri.parse("content://docs/planta.pdf"))
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals("O ficheiro é demasiado grande (máximo 20 MB).", viewModel.uiState.value.message)
    assertTrue(viewModel.uiState.value.documentos.isEmpty())
  }

  @Test
  fun removingAPickedFileDeletesItBeforeSaving() = runTest(dispatcher) {
    val repository = FakeDocumentoRepository(staged = pdf)
    val viewModel = ClientFormViewModel(repository, { null }, null)

    viewModel.onDocumentPicked(Uri.parse("content://docs/planta.pdf"))
    dispatcher.scheduler.advanceUntilIdle()
    viewModel.onRemoveDocumento(pdf)
    dispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.documentos.isEmpty())
    assertEquals(listOf("file:/planta.pdf"), repository.deletedFiles)
  }
}

private class FakeDocumentoRepository(
  private val staged: Documento? = null,
  private val error: Throwable? = null,
) : ClienteRepository {
  val deletedFiles = mutableListOf<String>()

  override val clientes: Flow<List<Cliente>> = flowOf(emptyList())

  override val clientesComFotos: Flow<List<ClienteComFotos>> = flowOf(emptyList())

  override fun observeCliente(id: String): Flow<ClienteComFotos?> = flowOf(null)

  override suspend fun getCliente(id: String): ClienteComFotos? = null

  override suspend fun stagePhoto(source: Uri): Foto = error("Not used in this test")

  override suspend fun stageDocument(source: Uri, nome: String): Documento {
    error?.let { throw it }
    return staged ?: error("Not used in this test")
  }

  override fun createPhotoFile(): File = error("Not used in this test")

  override suspend fun deletePhotoFile(foto: Foto) = Unit

  override suspend fun deleteDocumentoFile(documento: Documento) {
    deletedFiles.add(documento.uriLocal)
  }

  override suspend fun save(draft: ClienteDraft): String = error("Not used in this test")

  override suspend fun deleteCliente(id: String) = Unit

  override val syncStatus = flowOf(SyncStatus.IDLE)

  override val syncFault: Flow<SyncFault?> = flowOf(null)

  override suspend fun syncNow(): SyncStatus = SyncStatus.IDLE
}
