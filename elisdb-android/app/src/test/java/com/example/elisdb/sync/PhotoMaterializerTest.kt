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

  private fun pngBytes(): ByteArray =
    byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) +
      byteArrayOf(1, 2, 3)

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
  fun `declared mime mismatch is rejected and the previous file is kept`() = runBlocking {
    val store = FakePhotoStore().apply { stage("file:/a.jpg", "aGk=") }
    val m = PhotoMaterializer(store)
    val png = pngBytes()

    try {
      m.materialize(
        existing,
        server(PhotoHashes.sha256(png), mime = "image/jpeg"),
        download(png),
      )
      error("expected a mismatch")
    } catch (_: PhotoMimeMismatchException) {
      // expected
    }

    assertEquals("nothing was stored", 0, store.storeCalls)
    assertEquals("previous file is untouched", "aGk=", store.contentB64("file:/a.jpg"))
  }
}
