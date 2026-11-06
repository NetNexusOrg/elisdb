package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.AuthCause
import com.example.elisdb.remote.AuthException
import com.example.elisdb.remote.ClienteOutDto
import com.example.elisdb.remote.ConfigException
import com.example.elisdb.remote.FotoOutDto
import com.example.elisdb.remote.PullResponseDto
import com.example.elisdb.remote.PushResultDto
import com.example.elisdb.remote.TombstoneDto
import java.io.File
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
    api: FakeSyncApi = FakeSyncApi(),
    photoStore: FakePhotoStore = FakePhotoStore(),
    pullWatermark: PullWatermark? = null,
    incrementalPullEnabled: Boolean = false,
    clockOffsetStore: FakeClockOffsetStore? = null,
    photoPushState: FakePhotoPushState? = null,
    serverIdentityStore: FakeServerIdentityStore? = null,
    maxPhotosPerClient: Int = 500,
    onWarning: (String) -> Unit = {},
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
      serverIdentityStore = serverIdentityStore,
      maxPhotosPerClient = maxPhotosPerClient,
      onWarning = onWarning,
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
  fun `push reuses an idempotency key while batch content is unchanged`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true)))
    val api =
      FakeSyncApi().apply {
        pushResult = { records -> records.map { PushResultDto(it.client.id, "error") } }
      }
    val c = coord(dao, api = api)

    c.sync()
    c.sync()

    assertEquals(2, api.pushIdempotencyKeys.size)
    assertTrue("every batch carries a key", api.pushIdempotencyKeys.all { it != null })
    assertEquals(
      "an unchanged batch keeps its key across retries",
      api.pushIdempotencyKeys[0],
      api.pushIdempotencyKeys[1],
    )
  }

  @Test
  fun `push changes the idempotency key after a record edit`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, updatedAt = 100)))
    val api =
      FakeSyncApi().apply {
        pushResult = { records -> records.map { PushResultDto(it.client.id, "error") } }
      }
    val c = coord(dao, api = api)

    c.sync()
    val firstKey = api.pushIdempotencyKeys.single()
    dao.update(dao.data.first().copy(updatedAt = 200))
    c.sync()
    val secondKey = api.pushIdempotencyKeys.last()

    assertNotEquals(firstKey, secondKey)
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
  fun `push defers a client when a photo file is missing and warns`() = runBlocking {
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

    assertFalse("missing-file photo must not be marked for deletion", fotoDao.data.first { it.id == "f2" }.deletePending)
    assertFalse("present photo not marked", fotoDao.data.first { it.id == "f1" }.deletePending)
    assertEquals("one warning logged", 1, warnings)
    assertTrue("client push deferred", api.pushed.isEmpty())
    assertEquals("no push call for the blocked client", 0, api.pushCalls)
    assertTrue("client remains pending", dao.data.first { it.id == "1" }.pending)
    assertEquals("no watermark advance on empty delta pull", 5L, watermark.value)
  }

  @Test
  fun `unavailable server photo keeps the local copy and skips download`() = runBlocking {
    val local = cliente("1", nome = "Ana", updatedAt = 1L)
    val foto = Foto(id = "f1", clienteId = "1", uriLocal = "file:/local.webp", mime = "image/webp")
    val dao = FakeClienteDao(listOf(local))
    val fotoDao = FakeFotoDao(listOf(foto))
    val photoStore = FakePhotoStore().apply { stage("file:/local.webp", "aGk=") }
    val api =
      FakeSyncApi().apply {
        pullResponse =
          pullResponseOf(
            serverItem(
              id = "1",
              nome = "Ana",
              updatedAt = 100L,
              fotos = listOf(FotoOutDto(id = "f1", mime = "image/webp", available = false)),
            )
          )
      }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    c.sync()

    assertEquals(0, api.photoDownloadCalls)
    assertTrue("local photo row preserved", fotoDao.data.any { it.id == "f1" })
    assertTrue("local photo file preserved", photoStore.files.contains("file:/local.webp"))
  }

  @Test
  fun `incomplete server photo list preserves absent local photos`() = runBlocking {
    val local = cliente("1", nome = "Ana", updatedAt = 1L)
    val foto = Foto(id = "f1", clienteId = "1", uriLocal = "file:/local.webp", mime = "image/webp")
    val dao = FakeClienteDao(listOf(local))
    val fotoDao = FakeFotoDao(listOf(foto))
    val photoStore = FakePhotoStore().apply { stage("file:/local.webp", "aGk=") }
    var warnings = 0
    val api =
      FakeSyncApi().apply {
        pullResponse =
          pullResponseOf(
            serverItem(id = "1", nome = "Ana", updatedAt = 100L, fotos = emptyList(), photosComplete = false)
          )
      }
    val c =
      SyncCoordinator(
        api = api,
        clienteDao = dao,
        fotoDao = fotoDao,
        photoStore = photoStore,
        onWarning = { warnings++ },
      )

    c.sync()

    assertEquals("one warning for the incomplete list", 1, warnings)
    assertTrue("local photo row preserved", fotoDao.data.any { it.id == "f1" })
    assertTrue("local photo file preserved", photoStore.files.contains("file:/local.webp"))
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
  fun `photo sweeper only removes files not referenced by any row and respects the cap`() = runBlocking {
    val referenced = Foto(id = "f1", clienteId = "c1", uriLocal = "file:/keep.jpg")
    val photoStore =
      FakePhotoStore().apply {
        files.add("file:/keep.jpg")
        files.addAll((1..60).map { "file:/orphan$it.jpg" })
      }
    val sweeper = PhotoSweeper(FakeFotoDao(listOf(referenced)), photoStore)

    val removed = sweeper.sweep(maxFiles = 10)

    assertEquals(10, removed)
    assertTrue("referenced file kept", "file:/keep.jpg" in photoStore.files)
    assertEquals("orphans beyond the cap remain for a later sweep", 50, photoStore.files.count { it.startsWith("file:/orphan") })
  }

  @Test
  fun `photo sweeper drains many orphans in bounded batches`() = runBlocking {
    val referenced = Foto(id = "f1", clienteId = "c1", uriLocal = "file:/keep.jpg")
    val photoStore =
      FakePhotoStore().apply {
        files.add("file:/keep.jpg")
        files.addAll((1..120).map { "file:/orphan$it.jpg" })
      }
    val sweeper = PhotoSweeper(FakeFotoDao(listOf(referenced)), photoStore)

    val removed = sweeper.sweep()

    assertEquals(120, removed)
    assertTrue("referenced file kept", "file:/keep.jpg" in photoStore.files)
    assertTrue("all orphans reclaimed", photoStore.files.none { it.startsWith("file:/orphan") })
  }

  @Test
  fun `photo sweeper skips staged files until they are no longer registered`() = runBlocking {
    val staged = "file:/staged.jpg"
    val orphan = "file:/orphan.jpg"
    val photoStore =
      FakePhotoStore().apply {
        files.add(staged)
        files.add(orphan)
      }
    val registry = FakeStagedPhotoRegistry(setOf(staged))
    val sweeper = PhotoSweeper(FakeFotoDao(), photoStore, registry)

    assertEquals(1, sweeper.sweep())
    assertTrue("staged file kept", staged in photoStore.files)
    assertFalse("orphan removed", orphan in photoStore.files)

    registry.remove(staged)
    assertEquals("staged file becomes sweepable after removal", 1, sweeper.sweep())
    assertFalse(staged in photoStore.files)
  }

  @Test
  fun `photo sweeper leaves young files alone and removes old orphans`() = runBlocking {
    val young = File.createTempFile("elisdb-young", ".jpg").apply { writeText("young") }
    val old = File.createTempFile("elisdb-old", ".jpg").apply {
      writeText("old")
      setLastModified(System.currentTimeMillis() - 2 * 60 * 60 * 1000)
    }
    try {
      val photoStore =
        FakePhotoStore().apply {
          files.add(young.absolutePath)
          files.add(old.absolutePath)
        }
      val sweeper = PhotoSweeper(FakeFotoDao(), photoStore, minAgeSeconds = 60 * 60)

      assertEquals(1, sweeper.sweep())
      assertTrue("young file kept during grace period", young.absolutePath in photoStore.files)
      assertFalse("old orphan removed", old.absolutePath in photoStore.files)
    } finally {
      young.delete()
      old.delete()
    }
  }

  @Test
  fun `pull preserves a local edit newer than the tombstone and re-queues it`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = false, updatedAt = 1_000, nome = "Maria")))
    val api =
      FakeSyncApi().apply {
        pullResponse =
          pullResponseOf(
            tombstones = listOf(TombstoneDto(id = "1", deletedAt = 500, deliveryTs = 900)),
          )
      }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    val local = dao.data.single { it.id == "1" }
    assertTrue("newer local edit is kept and queued for push", local.pending)
    assertFalse("newer local edit is not marked as a pending deletion", local.deletePending)
    assertFalse("newer local edit is not demoted to local-only", local.localOnly)
  }

  @Test
  fun `pull removes a local record older than the tombstone`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = false, updatedAt = 100, nome = "Maria")))
    val api =
      FakeSyncApi().apply {
        pullResponse =
          pullResponseOf(
            tombstones = listOf(TombstoneDto(id = "1", deletedAt = 500, deliveryTs = 900)),
          )
      }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    assertTrue("older local record is deleted", dao.data.none { it.id == "1" })
  }

  private class FakeStagedPhotoRegistry(initial: Set<String> = emptySet()) : StagedPhotoRegistry {
    private val paths = initial.toMutableSet()

    override fun add(path: String) {
      paths += path
    }

    override fun remove(path: String) {
      paths -= path
    }

    override fun paths(): Set<String> = paths.toSet()

    override fun pruneOlderThan(maxAgeMillis: Long) = Unit

    override fun clear() {
      paths.clear()
    }
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
  fun `stale ack keeps the watermark and refreshes the winning server version`() = runBlocking {
    val dao =
      FakeClienteDao(
        listOf(
          cliente("1", pending = true, nome = "Maria"),
          cliente("2", pending = true, nome = "Joao"),
        )
      )
    val api =
      FakeSyncApi().apply {
        pushResult = { it.map { r -> PushResultDto(r.client.id, "stale") } }
        clientsResponse =
          listOf(
            ClienteOutDto(id = "1", nome = "Maria", updatedAt = 700),
            ClienteOutDto(id = "2", nome = "Joao", updatedAt = 700),
          )
        pullResponse = pullResponseOf()
      }
    val watermark = FakePullWatermark(value = 500L)
    val c = coord(clienteDao = dao, api = api, pullWatermark = watermark, incrementalPullEnabled = true)

    c.sync()

    assertEquals("stale must not clear the watermark", 0, watermark.clears)
    assertEquals("pull stays incremental", listOf<Long?>(500L), api.pulledSinces)
    assertEquals("watermark kept", 500L, watermark.value)
    assertEquals("server version applied", "MARIA", dao.data.first { it.id == "1" }.nome)
    assertEquals("server version applied", "JOAO", dao.data.first { it.id == "2" }.nome)
  }

  @Test
  fun `server latest below the watermark forces a full pull`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("local", pending = false, updatedAt = 100L)))
    val api =
      FakeSyncApi().apply {
        latestUpdatedAt = 300L
        pullResponse = pullResponseOf(serverItem(id = "s1", updatedAt = 200))
      }
    val watermark = FakePullWatermark(value = 500L)
    var warning: String? = null
    val c =
      coord(
        clienteDao = dao,
        api = api,
        pullWatermark = watermark,
        incrementalPullEnabled = true,
        onWarning = { warning = it },
      )

    c.sync()

    assertEquals("full pull requested", listOf<Long?>(null), api.pulledSinces)
    assertEquals("watermark reset once", 1, watermark.clears)
    assertEquals("watermark follows the restored server", 200L, watermark.value)
    assertTrue("restore warning emitted", warning?.contains("restaurado") == true)
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
    assertEquals("every page requests the fixed page size", listOf(200, 200), api.pulledLimits)
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
    val api = FakeSyncApi().apply { serverPhotos += "f1" }
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
        failUpload = java.io.IOException("upload failed")
        pushResult = { records ->
          records.map { PushResultDto(it.client.id, "accepted", missingPhotos = listOf("p1")) }
        }
      }
    val c =
      coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = store, photoPushState = state)

    c.sync()

    assertEquals(
      "a failed upload must not be recorded as stored",
      emptyMap<String, String>(),
      state.getHashes("1"),
    )
    assertTrue("client stays pending until the photo is stored", dao.data.first { it.id == "1" }.pending)
    assertEquals("one push attempt", 1, api.pushCalls)

    // The next sync retries the upload; once the server stores it, the client
    // is acknowledged and the hash is recorded.
    api.failUpload = null
    api.pullResponse = pullResponseOf(serverItem(id = "1", nome = "C1", updatedAt = 100))

    c.sync()

    assertEquals("photo re-uploaded on retry", 1, api.uploaded.size)
    assertEquals("retry push", 2, api.pushCalls)
    assertFalse("client acknowledged after retry", dao.data.first { it.id == "1" }.pending)
    assertEquals(setOf("p1"), state.getHashes("1").keys)
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
  fun `push splits more records than one batch`() = runBlocking {
    val dao = FakeClienteDao((1..201).map { cliente("$it", pending = true, nome = "C$it") })
    val api = FakeSyncApi().apply {
      maxPushRecords = 100
      latestUpdatedAt = 0
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    assertEquals(listOf(100, 100, 1), api.pushed.map { it.size })
    assertTrue("every record acknowledged across batches", dao.data.none { it.pending })
  }

  @Test
  fun `a rejected batch is reduced until the bad record is isolated`() = runBlocking {
    val dao = FakeClienteDao((1..3).map { cliente("$it", pending = true, nome = "C$it") })
    val api = FakeSyncApi().apply {
      maxAcceptedBatch = 2
      latestUpdatedAt = 0
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    assertEquals("the oversized batch is split down to single records", listOf(1, 1, 1), api.pushed.map { it.size })
    assertTrue("all valid records accepted", dao.data.none { it.pending })
  }

  @Test
  fun `a client above the photo limit is skipped before any upload`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Ana")))
    val fotoDao = FakeFotoDao((1..501).map { Foto(id = "f$it", clienteId = "1", uriLocal = "file:/f$it.jpg") })
    val api = FakeSyncApi().apply { latestUpdatedAt = 0 }
    var warning: String? = null
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, onWarning = { warning = it })

    c.sync()

    assertEquals("no push is attempted for the oversized client", 0, api.pushCalls)
    assertTrue("no photo upload is attempted", api.uploaded.isEmpty())
    assertTrue("client remains pending", dao.data.single { it.id == "1" }.pending)
    assertTrue("actionable warning surfaced", warning?.contains("501") == true)
  }

  @Test
  fun `a rejected push does not upload its photos`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Ana")))
    val fotoDao = FakeFotoDao(listOf(Foto(id = "f1", clienteId = "1", uriLocal = "file:/f1.jpg")))
    val photoStore = FakePhotoStore().apply { stage("file:/f1.jpg", "aGk=") }
    val api = FakeSyncApi().apply {
      maxAcceptedBatch = 0
      latestUpdatedAt = 0
    }
    val c = coord(clienteDao = dao, fotoDao = fotoDao, api = api, photoStore = photoStore)

    c.sync()

    assertEquals("no upload before a push is accepted", 0, api.uploaded.size)
    assertTrue("client remains pending", dao.data.single { it.id == "1" }.pending)
  }

  @Test
  fun `an incomplete full pull preserves local records`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("local", pending = false, nome = "Local")))
    val api = FakeSyncApi().apply {
      pullResponse = PullResponseDto(nextSince = 100, nextSinceId = null)
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    assertTrue("local record preserved after an incomplete pull", dao.data.any { it.id == "local" })
    assertFalse("incomplete pull does not mark the record local-only", dao.data.single { it.id == "local" }.localOnly)
  }

  @Test
  fun `a completed empty pull preserves local records as local-only`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("local", pending = false, nome = "Local")))
    val api = FakeSyncApi().apply {
      latestUpdatedAt = 0
      pullResponse = PullResponseDto()
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    val local = dao.data.singleOrNull { it.id == "local" }
    assertTrue("record preserved", local != null)
    assertTrue("record exposed as local-only", local!!.localOnly)
  }

  @Test
  fun `explicit replacement snapshots before deleting local-only records`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("local", pending = false, nome = "Local")))
    val api = FakeSyncApi().apply {
      latestUpdatedAt = 0
      pullResponse = PullResponseDto()
    }
    val c = coord(clienteDao = dao, api = api)
    var snapshotTaken = false

    val outcome = c.replaceLocalWithServer { snapshotTaken = true }

    assertEquals(SyncOutcome.Success, outcome)
    assertTrue("snapshot runs before deletion", snapshotTaken)
    assertTrue("local-only record removed only on explicit replacement", dao.data.none { it.id == "local" })
  }

  @Test
  fun `a failed snapshot aborts destructive replacement`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("local", pending = false, nome = "Local")))
    val api = FakeSyncApi().apply { pullResponse = PullResponseDto() }
    val c = coord(clienteDao = dao, api = api)

    val outcome = c.replaceLocalWithServer { throw java.io.IOException("no space") }

    assertEquals(SyncOutcome.TransientError, outcome)
    assertTrue("local data survives a failed snapshot", dao.data.any { it.id == "local" })
  }

  @Test
  fun `server identity change is reported and does not delete local data`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("local", pending = false, nome = "Local")))
    val identity = FakeServerIdentityStore()
    val api = FakeSyncApi().apply {
      instanceId = "server-a"
      latestUpdatedAt = 0
      pullResponse = PullResponseDto()
    }
    val c = coord(clienteDao = dao, api = api, serverIdentityStore = identity)

    c.sync()
    assertEquals("server-a", identity.value)

    api.instanceId = "server-b"
    val outcome = c.sync()

    assertTrue("identity change is reported", outcome is SyncOutcome.SuccessReport && outcome.identityChanged)
    assertEquals("new identity stored", "server-b", identity.value)
    assertTrue("local data preserved across an identity change", dao.data.any { it.id == "local" })
  }


  @Test
  fun `batch size never exceeds a server limit below the client default`() = runBlocking {
    val dao = FakeClienteDao((1..5).map { cliente("$it", pending = true, nome = "C$it") })
    val api = FakeSyncApi().apply {
      maxPushRecords = 2
      latestUpdatedAt = 0
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    assertEquals(listOf(2, 2, 1), api.pushed.map { it.size })
  }

  @Test
  fun `a failed batch does not prevent later batches`() = runBlocking {
    val dao = FakeClienteDao((1..201).map { cliente("$it", pending = true, nome = "C$it") })
    val api = FakeSyncApi().apply {
      maxPushRecords = 100
      failPushOnce = true
      latestUpdatedAt = 0
    }
    val c = coord(clienteDao = dao, api = api)

    c.sync()

    assertEquals("the first batch failed and the rest were sent", 2, api.pushed.size)
    assertEquals(100, api.pushed[0].size)
    assertEquals(1, api.pushed[1].size)
    assertEquals("only the failed batch stays pending", 100, dao.data.count { it.pending })
  }


  @Test
  fun `a client exactly at the photo limit is still pushed`() = runBlocking {
    val dao = FakeClienteDao(listOf(cliente("1", pending = true, nome = "Ana")))
    val fotoDao = FakeFotoDao(
      listOf(
        Foto(id = "f1", clienteId = "1", uriLocal = "file:/f1.jpg", mime = "image/jpeg", ordem = 0),
        Foto(id = "f2", clienteId = "1", uriLocal = "file:/f2.jpg", mime = "image/jpeg", ordem = 1),
      )
    )
    val photoStore = FakePhotoStore().apply {
      stage("file:/f1.jpg", "aGk=")
      stage("file:/f2.jpg", "aGk=")
    }
    val api = FakeSyncApi().apply { latestUpdatedAt = 0 }
    val c = coord(
      clienteDao = dao,
      fotoDao = fotoDao,
      api = api,
      photoStore = photoStore,
      maxPhotosPerClient = 2,
    )

    c.sync()

    assertEquals("the at-limit client is not rejected", 1, api.pushCalls)
    assertTrue("client acknowledged", !dao.data.single { it.id == "1" }.pending)
  }

}