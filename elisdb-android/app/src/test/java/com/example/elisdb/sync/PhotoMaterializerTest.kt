package com.example.elisdb.sync

import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.FotoOutDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMaterializerTest {

  private val existing =
    Foto(id = "f1", clienteId = "c1", uriLocal = "file:/a.jpg", mime = "image/jpeg", ordem = 0)

  private fun server(hash: String, mime: String = "image/jpeg", ordem: Int = 0) =
    FotoOutDto(id = "f1", mime = mime, hash = hash, ordem = ordem)

  private fun download(bytes: ByteArray): suspend (String) -> ByteArray = { bytes }

  @Test
  fun `same bytes are kept`() = runBlocking {
    val store = FakePhotoStore().apply { stage("file:/a.jpg", "aGk=") }
    val m = PhotoMaterializer(store)

    assertEquals(
      MaterializeResult.Keep("file:/a.jpg"),
      m.materialize(existing, server(sha256Of("aGk=")), download(decoded("aGk="))),
    )
    assertEquals("no rewrite when nothing changed", 0, store.storeCalls)
  }

  @Test
  fun `different bytes are materialized`() = runBlocking {
    val store = FakePhotoStore().apply { stage("file:/a.jpg", "aGk=") }
    val m = PhotoMaterializer(store)

    val result = m.materialize(existing, server(sha256Of("bmV3")), download(decoded("bmV3")))

    assertTrue(result is MaterializeResult.Repointed)
    assertEquals("rewrite when content differs", 1, store.storeCalls)
  }

  @Test
  fun `mime or ordem mismatch forces materialize`() = runBlocking {
    val store = FakePhotoStore().apply { stage("file:/a.jpg", "aGk=") }
    val m = PhotoMaterializer(store)

    assertEquals(
      "mime differs forces materialize",
      MaterializeResult.Repointed("file:/foto_0.png"),
      m.materialize(existing, server(sha256Of("aGk="), mime = "image/png"), download(decoded("aGk="))),
    )
    assertEquals(
      "ordem differs forces materialize",
      MaterializeResult.Repointed("file:/foto_1.jpg"),
      m.materialize(existing, server(sha256Of("aGk="), ordem = 1), download(decoded("aGk="))),
    )
  }
}
