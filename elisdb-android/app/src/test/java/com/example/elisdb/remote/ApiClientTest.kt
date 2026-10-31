package com.example.elisdb.remote

import com.example.elisdb.sync.ClockOffsetStore
import com.example.elisdb.sync.ClockSync
import com.example.elisdb.util.AuthHeaders
import com.example.elisdb.util.Signer
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ApiClientTest {

  private val signer = Signer.fromBase64("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=")
  private val json = Json { ignoreUnknownKeys = true }

  private fun client(engine: MockEngine, clock: ClockSync? = null): ApiClient {
    val http = HttpClient(engine) {
      install(ContentNegotiation) { json(json) }
      expectSuccess = false
    }
    return ApiClient("http://test", signer, http, clock)
  }

  private class InMemoryOffsetStore : ClockOffsetStore {
    var offset: Long? = null

    override fun getOffsetSeconds(): Long? = offset

    override fun setOffsetSeconds(offsetSeconds: Long) {
      offset = offsetSeconds
    }

    override fun clear() {
      offset = null
    }
  }

  private fun io.ktor.client.engine.mock.MockRequestHandleScope.okJson(body: String): io.ktor.client.request.HttpResponseData =
    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

  @Test
  fun push_returnsPerRecordStatuses() = runBlocking {
    val api = client(MockEngine { request ->
      // Verify auth headers are present.
      assertTrue(request.headers[AuthHeaders.TIMESTAMP] != null)
      assertTrue(request.headers[AuthHeaders.SIGNATURE] != null)
      okJson("""[{"id":"1","status":"accepted"},{"id":"2","status":"duplicate","existing_id":"3"}]""")
    })
    val results = api.push(
      listOf(
        RecordDto(ClienteInDto(id = "1", nome = "Ana", latitude = 0.0, longitude = 0.0, updatedAt = 1)),
        RecordDto(ClienteInDto(id = "2", nome = "Bia", latitude = 1.0, longitude = 1.0, updatedAt = 2)),
      ),
      timestamp = 1_700_000_000L,
    )
    assertEquals("accepted", results[0].status)
    assertEquals("duplicate", results[1].status)
    assertEquals("3", results[1].existingId)
  }

  @Test
  fun pull_decodesNullCoordinates() = runBlocking {
    val api = client(MockEngine {
      okJson("""{"records":[{"client":{"id":"c2","nome":"Bia","latitude":null,"longitude":null,"updated_at":2},"fotos":[]}],"tombstones":[]}""")
    })
    val items = api.pull(timestamp = 1_700_000_000L)
    assertEquals("c2", items.records[0].client.id)
    assertEquals(null, items.records[0].client.latitude)
    assertEquals(null, items.records[0].client.longitude)
  }

  @Test
  fun push_omitsNullCoordinatesFromPayload() = runBlocking {
    val api = client(MockEngine { request ->
      val body = request.body.toByteArray().decodeToString()
      assertTrue("latitude omitted when null", !body.contains("\"latitude\""))
      assertTrue("longitude omitted when null", !body.contains("\"longitude\""))
      assertTrue("nome still serialized", body.contains("\"nome\":\"Ana\""))
      okJson("""[{"id":"1","status":"accepted"}]""")
    })
    val results =
      api.push(
        listOf(RecordDto(ClienteInDto(id = "1", nome = "Ana", latitude = null, longitude = null, updatedAt = 1))),
        timestamp = 1_700_000_000L,
      )
    assertEquals("accepted", results[0].status)
  }

  @Test
  fun push_referencePhotoOmitsDataPayload() = runBlocking {
    val api = client(MockEngine { request ->
      val body = request.body.toByteArray().decodeToString()
      assertTrue("foto reference serialized without dataB64", !body.contains("dataB64"))
      assertTrue("foto id present", body.contains("\"id\":\"f1\""))
      okJson("""[{"id":"1","status":"accepted"}]""")
    })
    val results =
      api.push(
        listOf(
          RecordDto(
            ClienteInDto(id = "1", nome = "Ana", latitude = 0.0, longitude = 0.0, updatedAt = 1),
            fotos = listOf(FotoInDto(id = "f1", mime = "image/jpeg", ordem = 0)),
          )
        ),
        timestamp = 1_700_000_000L,
      )
    assertEquals("accepted", results[0].status)
  }

  @Test
  fun pull_decodesPhotos() = runBlocking {
    val api = client(MockEngine {
      okJson("""{"records":[{"client":{"id":"c1","nome":"Ana","latitude":0.0,"longitude":0.0,"updated_at":1},"fotos":[{"id":"f1","mime":"image/jpeg","hash":"abc123","ordem":0}]}],"tombstones":[]}""")
    })
    val items = api.pull(timestamp = 1_700_000_000L)
    assertEquals("c1", items.records[0].client.id)
    assertEquals("abc123", items.records[0].fotos[0].hash)
  }

  @Test
  fun pull_decodesTombstones() = runBlocking {
    val api = client(MockEngine {
      okJson("""{"records":[],"tombstones":[{"id":"gone","deleted_at":500}]}""")
    })
    val items = api.pull(timestamp = 1_700_000_000L)
    assertEquals(listOf(TombstoneDto(id = "gone", deletedAt = 500)), items.tombstones)
  }

  @Test
  fun pull_sendsSinceAsQueryParamWhenProvided() = runBlocking {
    var requestedUrl: String? = null
    val api = client(MockEngine { request ->
      requestedUrl = request.url.toString()
      okJson("""{"records":[],"tombstones":[]}""")
    })
    api.pull(timestamp = 1_700_000_000L, since = 123L)
    assertTrue(requestedUrl!!.contains("/api/pull?since=123"))
  }

  @Test
  fun pull_sendsLimitAsQueryParamWhenProvided() = runBlocking {
    var requestedUrl = ""
    val api = client(MockEngine { request ->
      requestedUrl = request.url.toString()
      okJson("""{"records":[],"tombstones":[]}""")
    })
    api.pull(timestamp = 1_700_000_000L, since = 123L, limit = 200)
    assertTrue(requestedUrl.contains("/api/pull?since=123&limit=200"))
  }

  @Test
  fun pull_withoutSinceOmitsQueryParam() = runBlocking {
    var requestedUrl = ""
    val api = client(MockEngine { request ->
      requestedUrl = request.url.toString()
      okJson("""{"records":[],"tombstones":[]}""")
    })
    api.pull(timestamp = 1_700_000_000L)
    assertTrue(requestedUrl.contains("/api/pull"))
    assertTrue(!requestedUrl.contains("since"))
  }

  @Test
  fun deleteOn204IsSuccess() = runBlocking {
    var sent = false
    val api = client(MockEngine {
      sent = true
      respond("", HttpStatusCode.NoContent)
    })
    assertTrue(api.delete("c1", timestamp = 1_700_000_000L))
    assertTrue(sent)
  }

  @Test(expected = AuthException::class)
  fun pushSurfaces401() = runBlocking<Unit> {
    val api = client(MockEngine {
      respond("unauthorized", HttpStatusCode.Unauthorized)
    })
    api.push(emptyList(), timestamp = 1_700_000_000L)
  }

  @Test
  fun auth401WithStaleClockDetailSparsesStaleClockCause() = runBlocking {
    val api = client(MockEngine {
      respond("""{"detail":"stale or future timestamp"}""", HttpStatusCode.Unauthorized)
    })
    try {
      api.pull(timestamp = 1_700_000_000L)
      throw AssertionError("expected AuthException")
    } catch (e: AuthException) {
      assertTrue(e.kind == AuthCause.STALE_CLOCK)
    }
  }

  @Test
  fun auth401WithInvalidSignatureDetailParsesInvalidKeyCause() = runBlocking {
    val api = client(MockEngine {
      respond("""{"detail":"invalid signature"}""", HttpStatusCode.Unauthorized)
    })
    try {
      api.pull(timestamp = 1_700_000_000L)
      throw AssertionError("expected AuthException")
    } catch (e: AuthException) {
      assertTrue(e.kind == AuthCause.INVALID_KEY)
    }
  }

  @Test
  fun requestWithoutResponseFailsOnTimeout() = runBlocking {
    val http =
      HttpClient(MockEngine {
        delay(5_000)
        respond("", HttpStatusCode.OK)
      }) {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
        install(HttpTimeout) {
          requestTimeoutMillis = 200
          connectTimeoutMillis = 200
          socketTimeoutMillis = 200
        }
      }
    val api = ApiClient("http://test", signer, http)
    try {
      api.pull(timestamp = 1_700_000_000L)
      throw AssertionError("expected a timeout failure, not a hanging request")
    } catch (expected: Exception) {
      // A stalled request SHALL fail (timeout) instead of hanging indefinitely.
    }
  }

  @Test
  fun fetchClientHistoryDecodesEntries() = runBlocking {
    var requestedPath: String? = null
    val api = client(MockEngine { request ->
      requestedPath = request.url.encodedPath
      assertTrue(request.headers[AuthHeaders.TIMESTAMP] != null)
      assertTrue(request.headers[AuthHeaders.SIGNATURE] != null)
      okJson("""[{"action":"create","username":"Maria","key_id":"k1","ts":100},{"action":"edit","username":null,"key_id":"k2","ts":200}]""")
    })
    val entries = api.fetchClientHistory("c1", timestamp = 1_700_000_000L)
    assertEquals("/api/clients/c1/history", requestedPath)
    assertEquals(2, entries.size)
    assertEquals("create", entries[0].action)
    assertEquals("Maria", entries[0].username)
    assertEquals("k1", entries[0].keyId)
    assertEquals(100L, entries[0].ts)
    assertEquals(null, entries[1].username)
  }

  @Test
  fun push_nonOkThrowsServerExceptionWithStatus() = runBlocking {
    val api = client(MockEngine { respond("boom", HttpStatusCode.InternalServerError) })
    try {
      api.push(emptyList(), timestamp = 1_700_000_000L)
      throw AssertionError("expected ServerException")
    } catch (e: ServerException) {
      assertEquals(500, e.status)
    }
  }

  @Test
  fun pull_nonOkThrowsServerExceptionWithStatus() = runBlocking {
    val api = client(MockEngine { respond("boom", HttpStatusCode.InternalServerError) })
    try {
      api.pull(timestamp = 1_700_000_000L)
      throw AssertionError("expected ServerException")
    } catch (e: ServerException) {
      assertEquals(500, e.status)
    }
  }

  @Test
  fun fetchServerTime_nonOkThrowsServerException() = runBlocking {
    val api = client(MockEngine { respond("down", HttpStatusCode.ServiceUnavailable) })
    try {
      api.fetchServerTime()
      throw AssertionError("expected ServerException")
    } catch (e: ServerException) {
      assertEquals(503, e.status)
    }
  }

  @Test
  fun uploadPhoto_non204ThrowsServerExceptionWithStatus() = runBlocking {
    val api = client(MockEngine { respond("too large", HttpStatusCode.PayloadTooLarge) })
    try {
      api.uploadPhoto("f1", byteArrayOf(1, 2, 3), timestamp = 1_700_000_000L)
      throw AssertionError("expected ServerException")
    } catch (e: ServerException) {
      assertEquals(413, e.status)
    }
  }

  @Test
  fun deleteOn404IsSuccess() = runBlocking {
    val api = client(MockEngine { respond("gone", HttpStatusCode.NotFound) })
    assertTrue(api.delete("c1", timestamp = 1_700_000_000L))
  }

  @Test
  fun deleteOnServerErrorReturnsFalse() = runBlocking {
    val api = client(MockEngine { respond("boom", HttpStatusCode.InternalServerError) })
    assertTrue(!api.delete("c1", timestamp = 1_700_000_000L))
  }

  @Test
  fun fetchKeyIdentitySendsSignedRequestAndDecodesIdentity() = runBlocking {
    var path: String? = null
    val api = client(MockEngine { request ->
      path = request.url.encodedPath
      assertTrue(request.headers[AuthHeaders.TIMESTAMP] != null)
      assertTrue(request.headers[AuthHeaders.SIGNATURE] != null)
      okJson("""{"key_id":"k1","username":"Maria"}""")
    })
    val identity = api.fetchKeyIdentity(timestamp = 1_700_000_000L)
    assertEquals("/api/keys/me", path)
    assertEquals("k1", identity.keyId)
    assertEquals("Maria", identity.username)
  }

  @Test
  fun staleClock_isCorrectedFromDateHeaderAndRetriedOnce() = runBlocking {
    val serverTime = 1_700_000_500L // Tue, 14 Nov 2023 22:21:40 GMT
    val seen = mutableListOf<Long>()
    val engine = MockEngine { request ->
      seen += request.headers[AuthHeaders.TIMESTAMP]!!.toLong()
      if (seen.size == 1) {
        respond(
          """{"detail":"stale or future timestamp"}""",
          HttpStatusCode.Unauthorized,
          headersOf(HttpHeaders.Date, "Tue, 14 Nov 2023 22:21:40 GMT"),
        )
      } else {
        okJson("""{"latest_updated_at":1,"client_count":2,"api_version":2}""")
      }
    }
    val store = InMemoryOffsetStore()
    val clock = ClockSync(store, deviceNowSeconds = { 1_600_000_000L })
    val api = client(engine, clock)

    val info = api.fetchServerInfo(timestamp = clock.nowSeconds())

    assertEquals(2, info.clientCount)
    assertEquals(2, info.apiVersion)
    assertEquals(listOf(1_600_000_000L, serverTime), seen)
    assertEquals(100_000_500L, store.offset)
    assertEquals(-100_000_500L, clock.skewSeconds.value)
  }

  @Test
  fun invalidKey_isReportedWithoutRetry() = runBlocking {
    var calls = 0
    val engine = MockEngine {
      calls++
      respond("""{"detail":"invalid signature"}""", HttpStatusCode.Unauthorized)
    }
    val api = client(engine, ClockSync(InMemoryOffsetStore(), deviceNowSeconds = { 100L }))
    try {
      api.fetchServerInfo(timestamp = 100L)
      fail("expected AuthException")
    } catch (e: AuthException) {
      assertEquals(AuthCause.INVALID_KEY, e.kind)
    }
    assertEquals(1, calls)
  }

  @Test
  fun serverDateHeader_keepsTheOffsetFreshOnSuccess() = runBlocking {
    val engine = MockEngine {
      respond(
        """{"latest_updated_at":1,"client_count":2}""",
        HttpStatusCode.OK,
        headersOf(
          HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
          HttpHeaders.Date to listOf("Tue, 14 Nov 2023 22:21:40 GMT"),
        ),
      )
    }
    val store = InMemoryOffsetStore()
    val clock = ClockSync(store, deviceNowSeconds = { 1_600_000_000L })
    val api = client(engine, clock)

    api.fetchServerInfo(timestamp = clock.nowSeconds())

    assertEquals(100_000_500L, store.offset)
    assertEquals(1_700_000_500L, clock.nowSeconds())
  }

  @Test
  fun historyParsesTheRecordedChanges() = runBlocking {
    val engine = MockEngine {
      okJson(
        """
        [{"action":"edit","username":"Joao","key_id":"k","ts":100,
          "details":{"fields":[{"field":"nome","old":"JOAO","new":"JOAO SILVA","truncated":false}],
                     "photos":{"added":["f1"],"removed":[],"reordered":false}}}]
        """
          .trimIndent()
      )
    }
    val api = client(engine)

    val entry = api.fetchClientHistory("5", timestamp = 100L).single()

    assertEquals("nome", entry.details?.fields?.single()?.field)
    assertEquals("JOAO SILVA", entry.details?.fields?.single()?.new)
    assertEquals(listOf("f1"), entry.details?.photos?.added)
  }

  @Test
  fun historyWithoutDetailsStillParses() = runBlocking {
    val engine = MockEngine {
      okJson("""[{"action":"create","username":"Joao","key_id":"k","ts":100}]""")
    }
    val api = client(engine)

    val entry = api.fetchClientHistory("5", timestamp = 100L).single()

    assertEquals("create", entry.action)
    assertEquals(null, entry.details)
  }

  @Test
  fun latestReleaseParsesThePublishedMetadata() = runBlocking {
    val engine = MockEngine {
      okJson(
        """{"version_name":"1.6.0","version_code":11,"size":1234,"sha256":"deadbeef","filename":"elisdb-1.6.0-11.apk"}"""
      )
    }
    val api = client(engine)

    val release = api.fetchLatestRelease(timestamp = 100L)

    assertEquals("1.6.0", release?.versionName)
    assertEquals(11, release?.versionCode)
    assertEquals(1234L, release?.size)
    assertEquals("elisdb-1.6.0-11.apk", release?.filename)
  }

  @Test
  fun latestReleaseIsNullWhenNothingIsPublished() = runBlocking {
    val engine =
      MockEngine { respond("""{"detail":"no release available"}""", HttpStatusCode.NotFound) }
    val api = client(engine)

    assertEquals(null, api.fetchLatestRelease(timestamp = 100L))
  }

  @Test
  fun downloadReleaseVerifiesThePublishedChecksum() = runBlocking {
    val bytes = "apk-bytes".toByteArray()
    val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    val release = AppReleaseDto("1.6.0", 11, bytes.size.toLong(), sha, "elisdb-1.6.0-11.apk")
    val engine = MockEngine { respond(bytes, HttpStatusCode.OK) }
    val api = client(engine)
    val dir = Files.createTempDirectory("release").toFile()

    val file = api.downloadRelease(release, dir, timestamp = 100L)

    assertArrayEquals(bytes, file.readBytes())
  }

  @Test
  fun downloadReleaseDeletesACorruptedPackage() = runBlocking {
    val bytes = "apk-bytes".toByteArray()
    val release = AppReleaseDto("1.6.0", 11, bytes.size.toLong(), "not-the-sha", "elisdb-1.6.0-11.apk")
    val engine = MockEngine { respond(bytes, HttpStatusCode.OK) }
    val api = client(engine)
    val dir = Files.createTempDirectory("release").toFile()

    try {
      api.downloadRelease(release, dir, timestamp = 100L)
      fail("expected ReleaseVerificationException")
    } catch (e: ReleaseVerificationException) {
      assertEquals("downloaded package does not match the published checksum", e.message)
    }
    assertEquals(0, dir.listFiles()?.size ?: 0)
  }
}
