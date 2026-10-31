package com.example.elisdb.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.data.local.Documento
import com.example.elisdb.sync.ClockOffsetStore
import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.fail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClienteRepositoryImplTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val dbName = "cliente_repository_test.db"

  private lateinit var database: AppDatabase
  private lateinit var repository: ClienteRepositoryImpl

  @Before
  fun setUp() {
    context.deleteDatabase(dbName)
    database = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
    repository = ClienteRepositoryImpl(context, database, database.clienteDao(), database.fotoDao())
  }

  @After
  fun tearDown() {
    database.close()
    context.deleteDatabase(dbName)
  }

  @Test
  fun saveCriaEEditaMesmoCliente() = runBlocking {
    val id =
      repository.save(
        ClienteDraft(nome = "Ana", latitude = 1.0, longitude = 2.0)
      )

    val created = repository.getCliente(id)
    assertEquals("ANA", created?.cliente?.nome)

    val updated =
      repository.save(
        ClienteDraft(id = id, nome = "Ana Costa", latitude = 1.0, longitude = 2.0)
      )
    assertEquals(id, updated)

    val after = repository.getCliente(id)
    assertEquals("ANA COSTA", after?.cliente?.nome)
    assertEquals("ANA COSTA", after?.cliente?.nome)
    assertNull(repository.getCliente("nao-existe"))
  }

  @Test
  fun saveGuardaNomeEmMaiusculasNaCriacao() = runBlocking {
    val id = repository.save(ClienteDraft(nome = "maRiA de sousa", latitude = 1.0, longitude = 2.0))
    assertEquals("MARIA DE SOUSA", repository.getCliente(id)?.cliente?.nome)
  }

  @Test
  fun saveGuardaNomeEmMaiusculasNaEdicao() = runBlocking {
    val id = repository.save(ClienteDraft(nome = "Ana", latitude = 1.0, longitude = 2.0))
    repository.save(ClienteDraft(id = id, nome = "ana costa", latitude = 1.0, longitude = 2.0))
    assertEquals("ANA COSTA", repository.getCliente(id)?.cliente?.nome)
  }

  @Test
  fun deleteClienteMarcaComoPendenteEApagaDaLista() = runBlocking {
    val id = repository.save(ClienteDraft(nome = "Bruno", latitude = 0.0, longitude = 0.0))

    repository.deleteCliente(id)

    assertNull(repository.getCliente(id))
  }

  @Test
  fun saveNovoClienteMarcaComoPendente() = runBlocking {
    val id = repository.save(ClienteDraft(nome = "Ana", latitude = 1.0, longitude = 2.0))
    val stored = database.clienteDao().getById(id)
    assertNotNull(stored)
    assertTrue(stored?.pending == true)
    assertFalse(stored?.deletePending == true)
  }

  @Test
  fun saveEdicaoRefrescaUpdatedAtEMantemPendente() = runBlocking {
    val id = repository.save(ClienteDraft(nome = "Ana", latitude = 1.0, longitude = 2.0))
    val primeiroUpdatedAt = database.clienteDao().getById(id)?.updatedAt

    Thread.sleep(5)
    repository.save(ClienteDraft(id = id, nome = "Ana Costa", latitude = 1.0, longitude = 2.0))

    val stored = database.clienteDao().getById(id)
    assertTrue(stored!!.updatedAt >= (primeiroUpdatedAt ?: 0L))
    assertTrue("edit should keep pending", stored.pending)
  }

  @Test
  fun ackLimpaMarcaPendente() = runBlocking {
    val id = repository.save(ClienteDraft(nome = "Ana", latitude = 1.0, longitude = 2.0))
    val stored = database.clienteDao().getById(id)!!
    database.clienteDao().update(stored.copy(pending = false))

    assertFalse(database.clienteDao().getPending().any { it.id == id })
  }

  @Test
  fun listaPorObservarOcultaPendentesDeEliminacao() = runBlocking {
    val id = repository.save(ClienteDraft(nome = "Bruno", latitude = 0.0, longitude = 0.0))
    database.clienteDao().update(database.clienteDao().getById(id)!!.copy(deletePending = true))

    assertFalse(database.clienteDao().getPendingDeletions().isEmpty())
    assertTrue(database.clienteDao().observeAll().first { true }.none { it.id == id })
  }

  @Test
  fun saveGuardaDocumentosNaOrdem() = runBlocking {
    val id =
      repository.save(
        ClienteDraft(
          nome = "Ana",
          documentos =
            listOf(
              Documento(id = "", clienteId = "", nome = "planta.pdf", uriLocal = "/f/planta.pdf"),
              Documento(id = "", clienteId = "", nome = "foto.jpg", mime = "image/webp", uriLocal = "/f/foto.webp"),
            ),
        )
      )

    val documentos = repository.getCliente(id)?.documentos.orEmpty()
    assertEquals(listOf("planta.pdf", "foto.jpg"), documentos.map { it.nome })
    assertEquals(listOf(0, 1), documentos.map { it.ordem })
    assertTrue("ids assigned on save", documentos.all { it.id.isNotEmpty() })
    assertEquals(listOf("planta.pdf", "foto.jpg"), documentos.sortedBy { it.ordem }.map { it.nome })
  }

  @Test
  fun saveSemDocumentoApagaLinhaEFicheiro() = runBlocking {
    val file = File(context.filesDir, "planta_${System.nanoTime()}.pdf").apply { writeBytes("%PDF".toByteArray()) }
    val id =
      repository.save(
        ClienteDraft(
          nome = "Ana",
          documentos = listOf(Documento(id = "", clienteId = "", nome = "planta.pdf", uriLocal = file.absolutePath)),
        )
      )
    assertTrue("file staged before save", file.exists())

    repository.save(ClienteDraft(id = id, nome = "Ana", documentos = emptyList()))

    assertTrue("document row removed", repository.getCliente(id)!!.documentos.isEmpty())
    assertFalse("document file removed after the commit", file.exists())
  }

  @Test
  fun stageDocumentCopiaPdfByteAByte() = runBlocking {
    val bytes = "%PDF-1.4\nconteudo\n%%EOF".toByteArray()
    val source = File(context.cacheDir, "origem_${System.nanoTime()}.pdf").apply { writeBytes(bytes) }

    val documento = repository.stageDocument(Uri.fromFile(source), "planta.pdf")

    assertEquals("planta.pdf", documento.nome)
    assertEquals("application/pdf", documento.mime)
    assertEquals("", documento.id)
    assertTrue("copied, not moved", source.exists())
    assertTrue("copy lives in the photos dir", File(documento.uriLocal).parentFile == File(context.filesDir, "photos"))
    assertTrue("bytes copied verbatim", File(documento.uriLocal).readBytes().contentEquals(bytes))
  }

  @Test
  fun stageDocumentRejeitaTipoNaoSuportado() = runBlocking {
    val source = File(context.cacheDir, "notas_${System.nanoTime()}.txt").apply { writeText("texto") }

    try {
      repository.stageDocument(Uri.fromFile(source), "notas.txt")
      fail("a .txt must be rejected")
    } catch (e: AttachmentRejectedException) {
      assertEquals(AttachmentRejection.UNSUPPORTED_TYPE, e.rejection)
    }
  }

  @Test
  fun stageDocumentRejeitaFicheiroAcimaDoLimite() = runBlocking {
    val source = File(context.cacheDir, "grande_${System.nanoTime()}.pdf")
    RandomAccessFile(source, "rw").use { it.setLength(ClienteRepositoryImpl.MAX_ATTACHMENT_BYTES + 1) }

    try {
      repository.stageDocument(Uri.fromFile(source), "grande.pdf")
      fail("a file above the ceiling must be rejected")
    } catch (e: AttachmentRejectedException) {
      assertEquals(AttachmentRejection.TOO_LARGE, e.rejection)
    }
  }

  @Test
  fun saveDeClienteInexistenteLancaClienteRemovido() = runBlocking {
    try {
      repository.save(ClienteDraft(id = "apagado-entretanto", nome = "Ana", latitude = 1.0, longitude = 2.0))
      org.junit.Assert.fail("save devia falhar para um cliente eliminado")
    } catch (e: ClienteRemovidoException) {
      assertEquals("apagado-entretanto", e.clienteId)
    }
  }

  @Test
  fun updatedAtUsaOffsetDoRelogioDoServidor() = runBlocking {
    val offsetSeconds = 3600L
    val repositoryWithOffset =
      ClienteRepositoryImpl(
        context,
        database,
        database.clienteDao(),
        database.fotoDao(),
        clockOffsetStore = FakeOffsetStore(offsetSeconds),
      )

    val before = System.currentTimeMillis() / 1000 + offsetSeconds
    val id = repositoryWithOffset.save(ClienteDraft(nome = "Ana", latitude = 1.0, longitude = 2.0))
    val after = System.currentTimeMillis() / 1000 + offsetSeconds

    val stored = database.clienteDao().getById(id)!!
    assertTrue(
      "updatedAt=${stored.updatedAt} devia estar entre $before e $after",
      stored.updatedAt in before..after,
    )
  }
}

private class FakeOffsetStore(private val offsetSeconds: Long?) : ClockOffsetStore {
  override fun getOffsetSeconds(): Long? = offsetSeconds

  override fun setOffsetSeconds(offsetSeconds: Long) = Unit

  override fun clear() = Unit
}
