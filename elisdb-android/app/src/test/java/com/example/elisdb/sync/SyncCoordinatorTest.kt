package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.Documento
import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.AuthCause
import com.example.elisdb.remote.AuthException
import com.example.elisdb.remote.ClienteOutDto
import com.example.elisdb.remote.ConfigException
import com.example.elisdb.remote.DocumentoOutDto
import com.example.elisdb.remote.FotoOutDto
import com.example.elisdb.remote.PullResponseDto
import com.example.elisdb.remote.PushResultDto
import com.example.elisdb.remote.TombstoneDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCoordinatorTest {

  private fun coord(
    clienteDao: FakeClienteDao,
    fotoDao: FakeFotoDao = FakeFotoDao(),
    documentoDao: FakeDocumentoDao? = null,
    api: FakeSyncApi = FakeSyncApi(),
    photoStore: FakePhotoStore = FakePhotoStore(),
    pullWatermark: PullWatermark? = null,
    incrementalPullEnabled: Boolean = false,
    clockOffsetStore: FakeClockOffsetStore? = null,
    photoPushState: FakePhotoPushState? = null,
    onError: (Throwable) -> Unit = {},
  ) =
    SyncCoordinator(
      api,
      clienteDao,
      fotoDao,
      photoStore,
      pullWatermark = pullWatermark,
      incrementalPullEnabled = incrementalPullEnabled,
      clockOffsetStore = clockOffsetStore,
      photoPushState = photoPushState,
      documentoDao = documentoDao,
      onError = onError,
    )

  private fun cliente(
    id: String,
    pending: Boolean = false,
    deletePending: Boolean = false,
    updatedAt: Long = 1L,
    nome: String = "C$id",
  ) = Cliente(
    id = id,
    nome = nome,
    latitude = 0.0,
    longitude = 0.0,
    dataCriacao = updatedAt,
    updatedAt = updatedAt,
    pending = pending,
    deletePending = deletePending,
  )

  @Test
  fun `push pending records cleared only after ack`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Ana")))
    val api = FakeSyncApi().apply {
      // After ack the server stores the record, so pull returns it.
      pullResponse = pullResponseOf(serverItem(id = "1", nome = "Ana", updatedAt = 1))
    }
    val c = coord(dao, api = api)

    c.sync()

    assertFalse("pending should be cleared after ack", dao.data.first { it.id == "1" }.pending)
    assertEquals(1, api.pushed.size)
  }

  @Test
  fun `offline push keeps data and reports error`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true)))
    val api = FakeSyncApi().apply { failWith = java.io.IOException("offline") }
    val c = coord(dao, api = api)

    val outcome = c.sync()

    assertEquals(SyncOutcome.TransientError, outcome)
    assertTrue("pending kept on failure", dao.data.first { it.id == "1" }.pending)
  }

  @Test
  fun `auth failure surfaces as AuthFailure`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true)))
    val api = FakeSyncApi().apply { failWith = AuthException("401", AuthCause.INVALID_KEY) }
    val c = coord(dao, api = api)

    assertEquals(SyncOutcome.AuthFailure, c.sync())
  }

  @Test
  fun `stale clock surfaces as ClockFailure`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true)))
    val api = FakeSyncApi().apply { failWith = AuthException("401", AuthCause.STALE_CLOCK) }
    val c = coord(dao, api = api)

    assertEquals(SyncOutcome.ClockFailure, c.sync())
  }

  @Test
  fun `config fault surfaces as ConfigFault`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true)))
    val api = FakeSyncApi().apply { failWith = ConfigException("no server configured") }
    val c = coord(dao, api = api)

    assertEquals(SyncOutcome.ConfigFault, c.sync())
    assertTrue("pending kept on config fault", dao.data.first { it.id == "1" }.pending)
  }

  @Test
  fun `delete transmitted and local record finalized after confirm`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("9", deletePending = true)))
    val foto = Foto(id = "f9", clienteId = "9", uriLocal = "file:/9.jpg")
    val fotoDao = FakeFotoDao(listOf(foto))
    val photoStore = FakePhotoStore()
    val api = FakeSyncApi().apply { deleteResult = { true } }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    c.sync()

    assertTrue(api.deleted.contains("9"))
    assertTrue("client should be permanently gone", dao.data.any { it.id == "9" }.not())
    assertTrue("foto should be gone", fotoDao.data.any { it.id == "f9" }.not())
  }

  @Test
  fun `duplicate merge preserves canonical photos already stored locally`() = runBlocking {
    val dao =
      FakeClienteDao(
        listOf(
          cliente("new", pending = true, nome = "Ana"),
          cliente("existing", pending = false, nome = "Velho"),
        )
      )
    val fotoDao =
      FakeFotoDao(
        listOf(
          Foto(id = "f1", clienteId = "new", uriLocal = "file:/new.jpg", mime = "image/jpeg", ordem = 0),
          Foto(id = "c1", clienteId = "existing", uriLocal = "file:/canonical.jpg", mime = "image/jpeg", ordem = 0),
        )
      )
    val photoStore =
      FakePhotoStore().apply {
        stage("file:/new.jpg", "bmV3")
        stage("file:/canonical.jpg", "Y2Fu")
      }
    val api =
      FakeSyncApi().apply {
        pushResult = {
          records ->
          records.map {
            PushResultDto(
              id = it.client.id,
              status = "duplicate",
              existingId = "existing",
              client = ClienteOutDto(id = "existing", nome = "Velho", latitude = 0.0, longitude = 0.0, updatedAt = 500),
              fotos = listOf(FotoOutDto(id = "c1", mime = "image/jpeg", hash = sha256Of("Y2Fu"), ordem = 0)),
            )
          }
        }
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    val outcome = c.sync()

    assertEquals(SyncOutcome.DuplicateMerged("Ana"), outcome)
    assertEquals("old id must be gone", 1, dao.data.size)
    val merged = dao.data.single()
    assertEquals("existing", merged.id)
    val fotos = fotoDao.getByCliente("existing")
    assertEquals("both photos kept on the canonical record", 2, fotos.size)
    assertTrue("local photo re-attached", fotos.any { it.id == "f1" && it.uriLocal == "file:/new.jpg" })
    assertTrue("canonical photo preserved", fotos.any { it.id == "c1" && it.uriLocal == "file:/canonical.jpg" })
    assertTrue("canonical photo file not re-materialized", photoStore.stored.none { it == "file:/canonical.jpg" })
  }

  @Test
  fun `duplicate merge materializes canonical photos not stored locally`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("new", pending = true, nome = "Ana")))
    val fotoDao = FakeFotoDao(listOf(Foto(id = "f1", clienteId = "new", uriLocal = "file:/new.jpg", mime = "image/jpeg", ordem = 0)))
    val photoStore =
      FakePhotoStore().apply {
        stage("file:/new.jpg", "bmV3")
      }
    val api =
      FakeSyncApi().apply {
        photos["c1"] = decoded("Y2Fu")
        pushResult = {
          records ->
          records.map {
            PushResultDto(
              id = it.client.id,
              status = "duplicate",
              existingId = "existing",
              client = ClienteOutDto(id = "existing", nome = "Velho", latitude = 0.0, longitude = 0.0, updatedAt = 500),
              fotos = listOf(FotoOutDto(id = "c1", mime = "image/jpeg", hash = sha256Of("Y2Fu"), ordem = 0)),
            )
          }
        }
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    c.sync()

    val fotos = fotoDao.getByCliente("existing")
    assertEquals("local photo re-attached", 2, fotos.size)
    assertTrue("local photo kept", fotos.any { it.id == "f1" })
    val canonicalFoto = fotos.first { it.id == "c1" }
    assertEquals("canonical photo materialized", "Y2Fu", photoStore.contentB64(canonicalFoto.uriLocal))
  }

  @Test
  fun `duplicate ack re-attaches record, keeps photos pending, and pull does not delete it`() =
    runBlocking {
      val dao = FakeClienteDao(listOf(cliente("new", pending = true, nome = "Ana")))
      val foto = Foto(id = "f1", clienteId = "new", uriLocal = "file:/new.jpg", mime = "image/jpeg", ordem = 0)
      val fotoDao = FakeFotoDao(listOf(foto))
      val photoStore =
        FakePhotoStore().apply {
          stage("file:/new.jpg", "bmV3")
        }
      val api =
        FakeSyncApi().apply {
          pushResult = { records -> records.map { PushResultDto(it.client.id, "duplicate", existingId = "existing") } }
          pullResponse = pullResponseOf(serverItem(id = "existing", nome = "Ana", updatedAt = 1))
        }
      val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

      val outcome = c.sync()

      assertEquals(SyncOutcome.DuplicateMerged("Ana"), outcome)
      assertTrue("old id must be gone", dao.data.any { it.id == "new" }.not())
      val merged = dao.data.single()
      assertEquals("existing", merged.id)
      assertTrue("merged record stays pending until re-pushed", merged.pending)
      assertTrue("photo re-associated to canonical id", fotoDao.data.all { it.clienteId == "existing" })
      assertTrue("pull must not delete the merged record", dao.data.any { it.id == "existing" })
    }

  @Test
  fun `duplicate ack with existing id that already exists locally replaces it in one record`() =
    runBlocking {
      val dao =
        FakeClienteDao(
          listOf(
            cliente("new", pending = true, nome = "Ana"),
            cliente("existing", pending = false, nome = "Velho"),
          )
        )
      val api =
        FakeSyncApi().apply {
          pushResult = { records -> records.map { PushResultDto(it.client.id, "duplicate", existingId = "existing") } }
        }
      val c = coord(clienteDao = dao, api = api)

      val outcome = c.sync()

      assertEquals(SyncOutcome.DuplicateMerged("Ana"), outcome)
      assertEquals("only the canonical record remains", 1, dao.data.size)
      val merged = dao.data.single()
      assertEquals("existing", merged.id)
      assertEquals("local fields kept", "Ana", merged.nome)
      assertTrue("merged record pending so it re-pushes", merged.pending)
    }

  @Test
  fun `pull overwrites records with no pending edits`() = runBlocking {
    val dao = FakeClienteDao(
      listOf(
        cliente("np1", pending = false, nome = "NoPending"),
      )
    )
    val api = FakeSyncApi().apply {
      pullResponse =
        pullResponseOf(
          serverItem(id = "np1", nome = "ServerName", updatedAt = 600L),
        )
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    assertEquals("non-pending overwritten", "SERVERNAME", dao.data.first { it.id == "np1" }.nome)
    assertEquals(600L, dao.data.first { it.id == "np1" }.updatedAt)
  }

  @Test
  fun `pull decodes photos to files`() = runBlocking {
    val dao = FakeClienteDao()
    val photoStore = FakePhotoStore()
    val api = FakeSyncApi().apply {
      photos["f1"] = decoded("aGk=")
      pullResponse =
        pullResponseOf(
          serverItem(
            id = "s1",
            fotos = listOf(FotoOutDto(id = "f1", mime = "image/jpeg", hash = sha256Of("aGk="))),
          )
        )
    }
    val c = coord(clienteDao = dao, api = api, photoStore = photoStore)

    c.sync()

    assertTrue(photoStore.stored.isNotEmpty())
  }

  @Test
  fun `pull maps created_at into dataCriacao and createdAt`() = runBlocking {
    val dao = FakeClienteDao()
    val api = FakeSyncApi().apply {
      pullResponse =
        pullResponseOf(serverItem(id = "s1", updatedAt = 500L, createdAt = 100L))
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    val local = dao.data.single()
    assertEquals("server created_at used for dataCriacao", 100L, local.dataCriacao)
    assertEquals("server created_at kept on createdAt", 100L, local.createdAt)
  }

  @Test
  fun `record without created_at preserves existing local dataCriacao`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("s1", pending = false, updatedAt = 300L)))
    val api = FakeSyncApi().apply {
      pullResponse =
        pullResponseOf(
          serverItem(
            id = "s1",
            nome = "ServerName",
            updatedAt = 600L,
            createdAt = null,
          )
        )
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    val local = dao.data.first { it.id == "s1" }
    assertEquals("updatedAt reflects server", 600L, local.updatedAt)
    assertEquals("local dataCriacao preserved when created_at absent", 300L, local.dataCriacao)
    assertEquals("createdAt mirrors preserved dataCriacao", 300L, local.createdAt)
  }

  @Test
  fun `record without created_at and without local copy falls back to updatedAt`() = runBlocking {
    val dao = FakeClienteDao()
    val api = FakeSyncApi().apply {
      pullResponse = pullResponseOf(serverItem(id = "new1", updatedAt = 700L, createdAt = null))
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    val local = dao.data.single()
    assertEquals(700L, local.dataCriacao)
    assertEquals(700L, local.createdAt)
  }

  @Test
  fun `incremental pull disabled sends no since although watermark exists`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("s1", pending = false, updatedAt = 100L)))
    val api = FakeSyncApi().apply {
      pullResponse = pullResponseOf(serverItem(id = "s1", updatedAt = 100L))
    }
    val watermark = FakePullWatermark(value = 50L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark)

    c.sync()

    assertEquals("gate keeps since unsent", listOf<Long?>(null), api.pulledSinces)
  }

  @Test
  fun `incremental pull with no watermark does a full pull then records it`() = runBlocking {
    val dao = FakeClienteDao()
    val api = FakeSyncApi().apply {
      pullResponse = pullResponseOf(serverItem(id = "s1", updatedAt = 120L))
    }
    val watermark = FakePullWatermark()
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertEquals("first pull is full", listOf<Long?>(null), api.pulledSinces)
    assertEquals("watermark advanced to max updated_at", 120L, watermark.value)
  }

  @Test
  fun `incremental pull sends watermark and advances it`() = runBlocking {
    val dao = FakeClienteDao()
    val api = FakeSyncApi().apply {
      pullResponse = pullResponseOf(serverItem(id = "s1", updatedAt = 700L), serverItem(id = "s2", updatedAt = 900L))
    }
    val watermark = FakePullWatermark(value = 600L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertEquals("delta pull sends since", listOf<Long?>(600L), api.pulledSinces)
    assertEquals("watermark advanced to max seen", 900L, watermark.value)
  }

  @Test
  fun `delta pull applies authoritative records and skips the local-drop sweep`() = runBlocking {
    val dao =
      FakeClienteDao(
        listOf(
          cliente("old", pending = false, updatedAt = 100L),
          cliente("np1", pending = false, nome = "NoPending", updatedAt = 100L),
        )
      )
    val api = FakeSyncApi().apply {
      pullResponse = pullResponseOf(serverItem(id = "np1", nome = "ServerName", updatedAt = 300L))
    }
    val watermark = FakePullWatermark(value = 200L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertEquals("delta applies authoritative record", "SERVERNAME", dao.data.first { it.id == "np1" }.nome)
    assertTrue("older local record not dropped on delta pull", dao.data.any { it.id == "old" })
  }

  @Test
  fun `push marks missing-file photos as pending delete and warns`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Ana")))
    val ok = Foto(id = "f1", clienteId = "1", uriLocal = "file:/ok.jpg")
    val gone = Foto(id = "f2", clienteId = "1", uriLocal = "file:/gone.jpg")
    val fotoDao = FakeFotoDao(listOf(ok, gone))
    val photoStore = FakePhotoStore().apply { stage("file:/ok.jpg", "aGk=") }
    val watermark = FakePullWatermark(value = 5L)
    val api = FakeSyncApi()
    var warnings = 0
    val c =
      SyncCoordinator(
        api = api,
        clienteDao = dao,
        fotoDao = fotoDao,
        photoStore = photoStore,
        pullWatermark = watermark,
        incrementalPullEnabled = true,
        onWarning = { warnings++ },
      )

    c.sync()

    assertTrue("missing-file photo marked pending delete", fotoDao.data.first { it.id == "f2" }.deletePending)
    assertFalse("present photo not marked", fotoDao.data.first { it.id == "f1" }.deletePending)
    assertEquals("one warning logged", 1, warnings)
    val pushedFotoIds = api.pushed.single().flatMap { it.fotos.map { f -> f.id } }
    assertEquals("only the readable photo is pushed", listOf("f1"), pushedFotoIds)
    assertTrue("client kept locally", dao.data.any { it.id == "1" && !it.pending })
    assertEquals("no watermark advance on empty delta pull", 5L, watermark.value)
  }

  @Test
  fun `post-pull sweep reclaims orphans but keeps materialized photos`() = runBlocking {
    val fotoDao = FakeFotoDao()
    val photoStore = FakePhotoStore().apply { files.add("file:/orphan.jpg") }
    val api =
      FakeSyncApi().apply {
        photos["f1"] = decoded("aGk=")
        pullResponse =
          pullResponseOf(
            serverItem(
              id = "s1",
              fotos = listOf(FotoOutDto(id = "f1", mime = "image/jpeg", hash = sha256Of("aGk="))),
            )
          )
      }
    val sweeper = PhotoSweeper(fotoDao, photoStore)
    val c = SyncCoordinator(api, FakeClienteDao(), fotoDao, photoStore, photoSweeper = sweeper)

    c.sync()

    assertTrue("materialized file kept", photoStore.stored.all { it in photoStore.files })
    assertFalse("orphan file reclaimed", "file:/orphan.jpg" in photoStore.files)
    assertEquals("exactly the pulled photo remains", 1, photoStore.stored.size)
  }

  @Test
  fun `photo sweeper only removes files not referenced by any row and is bounded`() = runBlocking {
    val referenced = Foto(id = "f1", clienteId = "c1", uriLocal = "file:/keep.jpg")
    val photoStore =
      FakePhotoStore().apply {
        files.add("file:/keep.jpg")
        files.addAll((1..60).map { "file:/orphan$it.jpg" })
      }
    val sweeper = PhotoSweeper(FakeFotoDao(listOf(referenced)), photoStore)

    val removed = sweeper.sweep()

    assertEquals(PhotoSweeper.MAX_ORPHANS_PER_SWEEP, removed)
    assertTrue("referenced file kept", "file:/keep.jpg" in photoStore.files)
    assertTrue("orphans beyond the batch remain for a later sweep", (51..60).all { "file:/orphan$it.jpg" in photoStore.files })
  }

  @Test
  fun `pull preserves photos of a client already stored locally`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("s1", pending = false, updatedAt = 300L)))
    val fotoDao =
      FakeFotoDao(listOf(Foto(id = "f1", clienteId = "s1", uriLocal = "file:/keep.jpg", mime = "image/jpeg", ordem = 0)))
    val photoStore = FakePhotoStore().apply { stage("file:/keep.jpg", "aGk=") }
    val api =
      FakeSyncApi().apply {
        pullResponse =
          pullResponseOf(serverItem(id = "s1", updatedAt = 600L, fotos = listOf(FotoOutDto(id = "f1", mime = "image/jpeg", hash = sha256Of("aGk="), ordem = 0))))
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    c.sync()

    assertTrue("existing photo row kept", fotoDao.data.any { it.id == "f1" && it.uriLocal == "file:/keep.jpg" })
    assertTrue("existing photo file kept", "file:/keep.jpg" in photoStore.files)
    assertEquals("client overwritten", 600L, dao.data.single { it.id == "s1" }.updatedAt)
  }

  @Test
  fun `pull keeps unchanged photo without rewriting it`() = runBlocking {
    val dao = FakeClienteDao()
    val fotoDao = FakeFotoDao(listOf(Foto("f1", "s1", "file:/keep.jpg", "image/jpeg", 0)))
    val photoStore = FakePhotoStore().apply { stage("file:/keep.jpg", "aGk=") }
    val api =
      FakeSyncApi().apply {
        pullResponse =
          pullResponseOf(serverItem(id = "s1", fotos = listOf(FotoOutDto(id = "f1", mime = "image/jpeg", hash = sha256Of("aGk="), ordem = 0))))
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    c.sync()

    assertEquals("unchanged photo not rewritten", 0, photoStore.storeCalls)
    assertTrue("row still points at the original file", fotoDao.data.any { it.id == "f1" && it.uriLocal == "file:/keep.jpg" })
    assertEquals("file content untouched", "aGk=", photoStore.contentB64("file:/keep.jpg"))
  }

  @Test
  fun `changed photo is materialized and old file removed after commit`() = runBlocking {
    val dao = FakeClienteDao()
    val fotoDao = FakeFotoDao(listOf(Foto("f1", "s1", "file:/old.jpg", "image/jpeg", 0)))
    val photoStore = FakePhotoStore().apply { stage("file:/old.jpg", "b2xk") }
    val api =
      FakeSyncApi().apply {
        photos["f1"] = decoded("bmV3")
        pullResponse =
          pullResponseOf(serverItem(id = "s1", fotos = listOf(FotoOutDto(id = "f1", mime = "image/jpeg", hash = sha256Of("bmV3"), ordem = 0))))
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    c.sync()

    assertEquals("changed photo rewritten once", 1, photoStore.storeCalls)
    val row = fotoDao.data.single()
    assertNotEquals("row now points at the new file", "file:/old.jpg", row.uriLocal)
    assertEquals("new content on disk", "bmV3", photoStore.contentB64(row.uriLocal))
    assertFalse("old file deleted after the row commit", "file:/old.jpg" in photoStore.files)
  }

  @Test
  fun `failed materialize leaves old file and row intact and reports transient error`() = runBlocking {
    val dao = FakeClienteDao()
    val fotoDao = FakeFotoDao(listOf(Foto("f1", "s1", "file:/old.jpg", "image/jpeg", 0)))
    val photoStore = FakePhotoStore().apply { stage("file:/old.jpg", "b2xk"); failStore = true }
    val api =
      FakeSyncApi().apply {
        photos["f1"] = decoded("bmV3")
        pullResponse =
          pullResponseOf(serverItem(id = "s1", fotos = listOf(FotoOutDto(id = "f1", mime = "image/jpeg", hash = sha256Of("bmV3"), ordem = 0))))
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    val outcome = c.sync()

    assertEquals(SyncOutcome.TransientError, outcome)
    assertTrue("old row referencing the old file intact", fotoDao.data.any { it.id == "f1" && it.uriLocal == "file:/old.jpg" })
    assertTrue("old file intact", "file:/old.jpg" in photoStore.files)
    assertEquals("old content intact", "b2xk", photoStore.contentB64("file:/old.jpg"))
  }

  @Test
  fun `transient photo download failure is retried and the pull completes`() = runBlocking {
    val dao = FakeClienteDao()
    val fotoDao = FakeFotoDao()
    val api =
      FakeSyncApi().apply {
        photos["f1"] = decoded("bmV3")
        photoFailures = 1
        pullResponse =
          pullResponseOf(
            serverItem(
              id = "s1",
              fotos = listOf(FotoOutDto(id = "f1", mime = "image/jpeg", hash = sha256Of("bmV3"), ordem = 0)),
            ),
          )
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api)

    val outcome = c.sync()

    assertEquals(SyncOutcome.Success, outcome)
    assertEquals("photo downloaded after one retry", 2, api.photoDownloadCalls)
    assertEquals("photo row stored", 1, fotoDao.data.size)
  }

  @Test
  fun `unexpected failure is reported to the error hook`() = runBlocking {
    val cause = java.io.IOException("boom")
    val api = FakeSyncApi().apply { failWith = cause }
    val reported = mutableListOf<Throwable>()
    val c = coord(clienteDao = FakeClienteDao(), api = api, onError = { reported += it })

    val outcome = c.sync()

    assertEquals(SyncOutcome.TransientError, outcome)
    assertEquals("cause surfaced for logging", listOf(cause), reported)
  }

  @Test
  fun `pull stores a client without coordinates`() = runBlocking {
    val dao = FakeClienteDao()
    val api = FakeSyncApi().apply { pullResponse = pullResponseOf(serverItem(id = "s1", nome = "Sem Local", latitude = null, longitude = null)) }
    val c = coord(dao, api = api)

    c.sync()

    val stored = dao.data.single { it.id == "s1" }
    assertEquals("SEM LOCAL", stored.nome)
    assertTrue("latitude null on pull", stored.latitude == null)
    assertTrue("longitude null on pull", stored.longitude == null)
  }

  @Test
  fun `pull uppercases a mixed-case server name locally`() = runBlocking {
    val dao = FakeClienteDao()
    val api = FakeSyncApi().apply { pullResponse = pullResponseOf(serverItem(id = "s1", nome = "maRiA de sousa")) }
    val c = coord(dao, api = api)

    c.sync()

    assertEquals("MARIA DE SOUSA", dao.data.single { it.id == "s1" }.nome)
  }

  @Test
  fun `push transmits a client without coordinates`() = runBlocking {
    val dao =
      FakeClienteDao(
        listOf(
          Cliente(
            id = "1",
            nome = "Sem Local",
            latitude = null,
            longitude = null,
            dataCriacao = 1,
            updatedAt = 1,
            pending = true,
          )
        )
      )
    val api = FakeSyncApi()
    val c = coord(dao, api = api)

    c.sync()

    assertEquals(1, api.pushed.size)
    val pushed = api.pushed.single().single().client
    assertTrue("latitude null on push", pushed.latitude == null)
    assertTrue("longitude null on push", pushed.longitude == null)
  }

  @Test
  fun `two pulls converge without extra rewrites`() = runBlocking {
    val dao = FakeClienteDao()
    val fotoDao = FakeFotoDao()
    val photoStore = FakePhotoStore()
    val api =
      FakeSyncApi().apply {
        photos["f1"] = decoded("bmV3")
        pullResponse =
          pullResponseOf(serverItem(id = "s1", fotos = listOf(FotoOutDto(id = "f1", mime = "image/jpeg", hash = sha256Of("bmV3"), ordem = 0))))
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    c.sync()
    c.sync()

    assertEquals("second pull recognises the unchanged photo", 1, photoStore.storeCalls)
    assertEquals("one row after convergence", 1, fotoDao.data.size)
  }

  @Test
  fun `sync refreshes clock offset from server time`() = runBlocking {
    val dao = FakeClienteDao()
    val offsetStore = FakeClockOffsetStore()
    val api =
      FakeSyncApi().apply {
        serverTime = System.currentTimeMillis() / 1000 + 90
      }
    val c = coord(clienteDao = dao, api = api, clockOffsetStore = offsetStore)

    c.sync()

    val offset = offsetStore.offset
    assertTrue("offset measured", offset != null && offset > 0)
  }

  @Test
  fun `sync without offset store still succeeds`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true)))
    val api = FakeSyncApi()
    val c = coord(clienteDao = dao, api = api)

    val outcome = c.sync()

    assertEquals(SyncOutcome.Success, outcome)
  }

  @Test
  fun `stale push surfaces the lost edit in the outcome`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Maria")))
    val api =
      FakeSyncApi().apply {
        pushResult = { it.map { r -> PushResultDto(r.client.id, "stale") } }
        pullResponse = pullResponseOf(serverItem(id = "1", nome = "MARIA", updatedAt = 2))
      }
    val c = coord(clienteDao = dao, api = api)

    val outcome = c.sync()

    assertTrue("outcome is SuccessWithStatuses", outcome is SyncOutcome.SuccessWithStatuses)
    val statuses = outcome as SyncOutcome.SuccessWithStatuses
    assertEquals(listOf("Maria"), statuses.stale)
    val stored = dao.data.first { it.id == "1" }
    assertTrue("pending cleared for server-owned record", stored.pending.not())
    assertEquals("server version applied", "MARIA", stored.nome)
  }

  @Test
  fun `pull removes a tombstoned local record without pending edits`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = false, nome = "Maria")))
    val api =
      FakeSyncApi().apply {
        pullResponse = pullResponseOf(tombstones = listOf(TombstoneDto(id = "1", deletedAt = 500)))
      }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    assertTrue("tombstoned record removed", dao.data.none { it.id == "1" })
  }

  @Test
  fun `pull keeps a tombstoned record with pending edits`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Maria")))
    val api =
      FakeSyncApi().apply {
        // Push returns no statuses, so the pending edit survives into the pull phase.
        pushResult = { emptyList() }
        pullResponse = pullResponseOf(tombstones = listOf(TombstoneDto(id = "1", deletedAt = 500)))
      }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    assertTrue("pending record kept for next push", dao.data.any { it.id == "1" && it.pending })
  }

  @Test
  fun `pull follows the pagination cursor across pages`() = runBlocking {
    val dao = FakeClienteDao()
    val watermark = FakePullWatermark()
    val api = FakeSyncApi()
    api.pullQueue.addAll(
      listOf(
        PullResponseDto(
          records = listOf(serverItem(id = "a", updatedAt = 100)),
          nextSince = 100,
          nextSinceId = "a",
        ),
        PullResponseDto(
          records = listOf(serverItem(id = "b", updatedAt = 200)),
          nextSince = null,
          nextSinceId = null,
        ),
      )
    )
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertTrue("first page applied", dao.data.any { it.id == "a" })
    assertTrue("second page applied", dao.data.any { it.id == "b" })
    assertEquals("watermark advanced to the last page", 200L, watermark.value)
    assertEquals("two pages fetched", 2, api.pulledSinces.size)
  }

  @Test
  fun `pull stops when the cursor does not advance`() = runBlocking {
    val api =
      FakeSyncApi().apply {
        // The server keeps claiming the same cursor; without a guard this loops forever.
        pullResponse = PullResponseDto(nextSince = 100, nextSinceId = null)
      }
    val c =
      coord(
        clienteDao = FakeClienteDao(),
        api = api,
        pullWatermark = FakePullWatermark(value = 100L),
        incrementalPullEnabled = true,
      )

    c.sync()

    assertEquals("stopped after the non-advancing page", 1, api.pulledSinces.size)
  }

  @Test
  fun `first push uploads photos and sends metadata`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Ana")))
    val fotoDao =
      FakeFotoDao(
        listOf(
          Foto(id = "f1", clienteId = "1", uriLocal = "file:/f1.jpg", mime = "image/jpeg", ordem = 0)
        )
      )
    val photoStore = FakePhotoStore().apply { stage("file:/f1.jpg", "aGk=") }
    val pushState = FakePhotoPushState()
    val api =
      FakeSyncApi().apply {
        // The server now holds the pushed record, so the full pull keeps it
        // (and its stored push hashes) instead of reconciling it away.
        pullResponse = pullResponseOf(serverItem(id = "1", nome = "ANA", updatedAt = 1))
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore, photoPushState = pushState)

    c.sync()

    val pushed = api.pushed.single().single()
    assertEquals("f1", pushed.fotos.single().id)
    assertEquals("photo uploaded as bytes", decoded("aGk=").toList(), api.uploaded["f1"]?.toList())
    assertEquals("first push stores hashes", sha256Of("aGk="), pushState.getHashes("1")["f1"])
  }

  @Test
  fun `second push skips uploading unchanged photos`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Ana")))
    val fotoDao =
      FakeFotoDao(
        listOf(
          Foto(id = "f1", clienteId = "1", uriLocal = "file:/f1.jpg", mime = "image/jpeg", ordem = 0)
        )
      )
    val photoStore = FakePhotoStore().apply { stage("file:/f1.jpg", "aGk=") }
    val pushState = FakePhotoPushState().apply { hashes["1"] = mapOf("f1" to PhotoHashes.sha256(byteArrayOf(0x68, 0x69))) }
    val api = FakeSyncApi()
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore, photoPushState = pushState)

    c.sync()

    val pushed = api.pushed.single().single()
    assertEquals("f1", pushed.fotos.single().id)
    assertTrue("unchanged photo not re-uploaded", "f1" !in api.uploaded)
  }

  @Test
  fun `ack does not overwrite an edit made while the push is in flight`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, updatedAt = 100, nome = "Antigo")))
    val api =
      FakeSyncApi().apply {
        pushResult = { records ->
          // The user saves the same client while the request is on the wire.
          val i = dao.data.indexOfFirst { it.id == "1" }
          dao.data[i] =
            dao.data[i].copy(
              nome = "Editado",
              updatedAt = 200,
              pending = true,
              // The repository bumps the revision on every local write.
              revision = dao.data[i].revision + 1,
            )
          records.map { PushResultDto(it.client.id, "accepted") }
        }
      }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    val local = dao.data.single { it.id == "1" }
    assertEquals("concurrent edit kept", "Editado", local.nome)
    assertTrue("concurrent edit stays pending", local.pending)
  }

  @Test
  fun `stale ack does not discard an edit made while the push is in flight`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, updatedAt = 100, nome = "Antigo")))
    val api =
      FakeSyncApi().apply {
        pushResult = { records ->
          val i = dao.data.indexOfFirst { it.id == "1" }
          dao.data[i] =
            dao.data[i].copy(
              nome = "Editado",
              updatedAt = 200,
              pending = true,
              // The repository bumps the revision on every local write.
              revision = dao.data[i].revision + 1,
            )
          records.map { PushResultDto(it.client.id, "stale") }
        }
      }
    val c = coord(clienteDao = dao, api = api)

    val outcome = c.sync()

    val local = dao.data.single { it.id == "1" }
    assertEquals("concurrent edit kept", "Editado", local.nome)
    assertTrue("concurrent edit stays pending", local.pending)
    assertTrue(
      "no edit reported as lost",
      outcome !is SyncOutcome.SuccessWithStatuses || outcome.stale.isEmpty(),
    )
  }

  @Test
  fun `photos the server could not find are not recorded as uploaded`() = runBlocking {
    val store = FakePhotoStore().apply { stage("file:/p1.webp", "cGhvdG8=") }
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, updatedAt = 100)))
    val fotoDao = FakeFotoDao(listOf(Foto(id = "p1", clienteId = "1", uriLocal = "file:/p1.webp")))
    val state = FakePhotoPushState()
    val api =
      FakeSyncApi().apply {
        pushResult = { records ->
          records.map { PushResultDto(it.client.id, "accepted", missingPhotos = listOf("p1")) }
        }
      }
    val c =
      coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = store, photoPushState = state)

    c.sync()

    assertEquals(
      "photo the server never stored must be uploaded again",
      emptyMap<String, String>(),
      state.getHashes("1"),
    )
  }

  @Test
  fun `pull watermark follows the delivery cursor instead of the device clock`() = runBlocking {
    val dao = FakeClienteDao()
    val api =
      FakeSyncApi().apply {
        pullResponse = pullResponseOf(serverItem(id = "1", updatedAt = 1_000, deliveryTs = 60))
      }
    val watermark = FakePullWatermark(value = 50L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertEquals("watermark follows delivery_ts", 60L, watermark.value)
  }

  @Test
  fun `skipped record holds the watermark back`() = runBlocking {
    // A recent pending edit still blocks the cursor: the server version cannot be
    // applied until the local edit is pushed.
    val recent = System.currentTimeMillis() / 1000 - 60
    val dao = FakeClienteDao(listOf(cliente("2", pending = true, updatedAt = recent)))
    val api =
      FakeSyncApi().apply {
        // The server rejects the pending record, so it stays pending into the pull.
        pushResult = { records -> records.map { PushResultDto(it.client.id, "error") } }
        pullResponse =
          pullResponseOf(
            serverItem(id = "1", updatedAt = 100, deliveryTs = 100),
            serverItem(id = "2", updatedAt = 200, deliveryTs = 200),
          )
      }
    val watermark = FakePullWatermark(value = 50L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertEquals("watermark stops below the skipped record", 199L, watermark.value)
  }

  @Test
  fun `deletion from a device with an older clock is still applied`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", updatedAt = 100)))
    val api =
      FakeSyncApi().apply {
        pullResponse =
          pullResponseOf(
            tombstones = listOf(TombstoneDto(id = "1", deletedAt = 100, deliveryTs = 900))
          )
      }
    val watermark = FakePullWatermark(value = 500L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertTrue("tombstone delivered by its delivery timestamp", dao.data.none { it.id == "1" })
    assertEquals(900L, watermark.value)
  }

  @Test
  fun `skipped tombstone holds the watermark back`() = runBlocking {
    val recent = System.currentTimeMillis() / 1000 - 60
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, updatedAt = recent)))
    val api =
      FakeSyncApi().apply {
        pushResult = { records -> records.map { PushResultDto(it.client.id, "error") } }
        pullResponse =
          pullResponseOf(
            serverItem(id = "9", updatedAt = 200, deliveryTs = 200),
            tombstones = listOf(TombstoneDto(id = "1", deletedAt = 50, deliveryTs = 150)),
          )
      }
    val watermark = FakePullWatermark(value = 100L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertTrue("pending record kept so its edit can be pushed", dao.data.any { it.id == "1" && it.pending })
    assertEquals("watermark stops below the skipped tombstone", 149L, watermark.value)
  }

  @Test
  fun `pending record older than a week no longer holds the watermark`() = runBlocking {
    // A record the server keeps rejecting (status=error) used to block the cursor
    // forever, so every pull re-downloaded everything after it. After a week it is
    // treated as stuck: the cursor moves on, the record stays pending and is retried.
    val eightDaysAgo = System.currentTimeMillis() / 1000 - 8L * 24 * 60 * 60
    val dao = FakeClienteDao(listOf(cliente("2", pending = true, updatedAt = eightDaysAgo)))
    val api =
      FakeSyncApi().apply {
        pushResult = { records -> records.map { PushResultDto(it.client.id, "error") } }
        pullResponse =
          pullResponseOf(
            serverItem(id = "1", updatedAt = 100, deliveryTs = 100),
            serverItem(id = "2", updatedAt = 200, deliveryTs = 200),
          )
      }
    val watermark = FakePullWatermark(value = 50L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertEquals("watermark moves past the stuck record", 200L, watermark.value)
    assertTrue("the record stays pending for a later retry", dao.data.any { it.id == "2" && it.pending })
  }

  @Test
  fun `stuck record does not hold the watermark for a skipped tombstone`() = runBlocking {
    val eightDaysAgo = System.currentTimeMillis() / 1000 - 8L * 24 * 60 * 60
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, updatedAt = eightDaysAgo)))
    val api =
      FakeSyncApi().apply {
        pushResult = { records -> records.map { PushResultDto(it.client.id, "error") } }
        pullResponse =
          pullResponseOf(
            serverItem(id = "9", updatedAt = 200, deliveryTs = 200),
            tombstones = listOf(TombstoneDto(id = "1", deletedAt = 50, deliveryTs = 150)),
          )
      }
    val watermark = FakePullWatermark(value = 100L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertEquals("watermark moves past the stuck record", 200L, watermark.value)
    assertTrue("the pending record is kept", dao.data.any { it.id == "1" && it.pending })
  }

  @Test
  fun `push uploads a document once per content change`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Ana")))
    val documento =
      Documento(id = "d1", clienteId = "1", nome = "planta.pdf", uriLocal = "file:/planta.pdf")
    val documentoDao = FakeDocumentoDao(listOf(documento))
    val photoStore = FakePhotoStore().apply { stage("file:/planta.pdf", "cGRm") }
    val api = FakeSyncApi()
    val c =
      coord(
        clienteDao = dao,
        documentoDao = documentoDao,
        api = api,
        photoStore = photoStore,
        pullWatermark = FakePullWatermark(value = 5L),
        incrementalPullEnabled = true,
        photoPushState = FakePhotoPushState(),
      )

    c.sync()

    assertEquals("document uploaded", 1, api.documentUploadCalls)
    assertEquals("pdf", String(api.uploadedDocuments.getValue("d1")))
    val pushed = api.pushed.single().single().documentos
    assertEquals("document listed in the record", listOf("d1"), pushed.map { it.id })
    assertEquals("planta.pdf", pushed.single().nome)

    // Editing the record without touching the file must not upload it again.
    dao.data[0] = dao.data[0].copy(pending = true, revision = dao.data[0].revision + 1)
    c.sync()

    assertEquals("unchanged document not re-uploaded", 1, api.documentUploadCalls)
  }

  @Test
  fun `push marks a missing-file document as pending delete and warns`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Ana")))
    val documento =
      Documento(id = "d1", clienteId = "1", nome = "planta.pdf", uriLocal = "file:/gone.pdf")
    val documentoDao = FakeDocumentoDao(listOf(documento))
    val api = FakeSyncApi()
    var warnings = 0
    val c =
      SyncCoordinator(
        api = api,
        clienteDao = dao,
        fotoDao = FakeFotoDao(),
        photoStore = FakePhotoStore(),
        pullWatermark = FakePullWatermark(value = 5L),
        documentoDao = documentoDao,
        onWarning = { warnings++ },
      )

    c.sync()

    assertTrue("missing-file document marked pending delete", documentoDao.data.single().deletePending)
    assertEquals("one warning logged", 1, warnings)
    assertTrue("record pushed without the missing document", api.pushed.single().single().documentos.isEmpty())
    assertTrue("client kept and synced", dao.data.any { it.id == "1" && !it.pending })
  }

  @Test
  fun `pull materializes a document next to the client`() = runBlocking {
    val dao = FakeClienteDao()
    val api =
      FakeSyncApi().apply {
        documents["d1"] = decoded("cGRm")
        pullResponse =
          pullResponseOf(
            serverItem(
              id = "1",
              updatedAt = 100,
              documentos =
                listOf(
                  DocumentoOutDto(
                    id = "d1",
                    nome = "planta.pdf",
                    mime = "application/pdf",
                    ordem = 0,
                    hash = sha256Of("cGRm"),
                  )
                ),
            )
          )
      }
    val documentoDao = FakeDocumentoDao()
    val photoStore = FakePhotoStore()
    val c = coord(clienteDao = dao, documentoDao = documentoDao, api = api, photoStore = photoStore)

    c.sync()

    val row = documentoDao.getByCliente("1").single()
    assertEquals("d1", row.id)
    assertEquals("planta.pdf", row.nome)
    assertEquals("application/pdf", row.mime)
    assertEquals("bytes stored", "cGRm", photoStore.contentB64(row.uriLocal))
  }

  @Test
  fun `pull keeps an unchanged document without downloading it`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", updatedAt = 100)))
    val existing =
      Documento(
        id = "d1",
        clienteId = "1",
        nome = "planta.pdf",
        mime = "application/pdf",
        uriLocal = "file:/keep.pdf",
        ordem = 0,
      )
    val documentoDao = FakeDocumentoDao(listOf(existing))
    val photoStore = FakePhotoStore().apply { stage("file:/keep.pdf", "cGRm") }
    val api =
      FakeSyncApi().apply {
        pullResponse =
          pullResponseOf(
            serverItem(
              id = "1",
              updatedAt = 100,
              documentos =
                listOf(
                  DocumentoOutDto(
                    id = "d1",
                    nome = "planta.pdf",
                    mime = "application/pdf",
                    ordem = 0,
                    hash = sha256Of("cGRm"),
                  )
                ),
            )
          )
      }
    val c = coord(clienteDao = dao, documentoDao = documentoDao, api = api, photoStore = photoStore)

    c.sync()

    assertEquals("no download for unchanged document", 0, api.documentDownloadCalls)
    assertEquals("file kept", "file:/keep.pdf", documentoDao.data.single().uriLocal)
    assertTrue("file still on disk", "file:/keep.pdf" in photoStore.files)
  }

  @Test
  fun `duplicate merge keeps the documents of both records`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("new", pending = true, nome = "Ana")))
    val documentoDao =
      FakeDocumentoDao(
        listOf(
          Documento(
            id = "d1",
            clienteId = "new",
            nome = "local.pdf",
            mime = "application/pdf",
            uriLocal = "file:/local.pdf",
          )
        )
      )
    val photoStore = FakePhotoStore().apply { stage("file:/local.pdf", "bG9j") }
    val api =
      FakeSyncApi().apply {
        documents["c1"] = decoded("Y2Fu")
        pushResult = { records ->
          records.map {
            PushResultDto(
              id = it.client.id,
              status = "duplicate",
              existingId = "existing",
              client = ClienteOutDto(id = "existing", nome = "Ana", latitude = 0.0, longitude = 0.0, updatedAt = 500),
              documentos =
                listOf(
                  DocumentoOutDto(
                    id = "c1",
                    nome = "canonical.pdf",
                    mime = "application/pdf",
                    ordem = 0,
                    hash = sha256Of("Y2Fu"),
                  )
                ),
            )
          }
        }
      }
    val c = coord(clienteDao = dao, documentoDao = documentoDao, api = api, photoStore = photoStore)

    c.sync()

    val documentos = documentoDao.getAllByCliente("existing")
    assertEquals("both documents kept on the canonical record", 2, documentos.size)
    assertTrue("local document re-attached", documentos.any { it.id == "d1" && it.clienteId == "existing" })
    val canonical = documentos.first { it.id == "c1" }
    assertEquals("canonical document materialized", "Y2Fu", photoStore.contentB64(canonical.uriLocal))
    assertEquals("canonical name kept", "canonical.pdf", canonical.nome)
  }

  @Test
  fun `deleting a client removes its document rows and files`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("9", deletePending = true)))
    val documento =
      Documento(id = "d9", clienteId = "9", nome = "planta.pdf", uriLocal = "file:/d9.pdf")
    val documentoDao = FakeDocumentoDao(listOf(documento))
    val photoStore = FakePhotoStore().apply { stage("file:/d9.pdf", "cGRm") }
    val api = FakeSyncApi().apply { deleteResult = { true } }
    val c = coord(clienteDao = dao, documentoDao = documentoDao, api = api, photoStore = photoStore)

    c.sync()

    assertTrue("document row deleted", documentoDao.data.isEmpty())
    assertFalse("document file deleted", "file:/d9.pdf" in photoStore.files)
  }

  @Test
  fun `document sweeper keeps referenced files and reclaims orphans`() = runBlocking {
    val documento =
      Documento(id = "d1", clienteId = "1", nome = "planta.pdf", uriLocal = "file:/keep.pdf")
    val photoStore =
      FakePhotoStore().apply {
        files.add("file:/keep.pdf")
        files.add("file:/orphan.pdf")
      }
    val sweeper = PhotoSweeper(FakeFotoDao(), photoStore, FakeDocumentoDao(listOf(documento)))

    val removed = sweeper.sweep()

    assertEquals("one orphan reclaimed", 1, removed)
    assertTrue("referenced document kept", "file:/keep.pdf" in photoStore.files)
    assertFalse("orphan document reclaimed", "file:/orphan.pdf" in photoStore.files)
  }
}