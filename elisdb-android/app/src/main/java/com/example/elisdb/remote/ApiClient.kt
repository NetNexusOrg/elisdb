package com.example.elisdb.remote

import com.example.elisdb.settings.validateServerUrl
import com.example.elisdb.util.AuthHeaders
import com.example.elisdb.util.Signer
import com.example.elisdb.sync.ClockSync
import com.example.elisdb.sync.ServerInfo
import com.example.elisdb.sync.SyncApi
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reason the server rejected an authenticated request. */
enum class AuthCause {
  /** Signing key not recognized or the signature did not verify. */
  INVALID_KEY,
  /** The signed timestamp lies outside the server's freshness window. */
  STALE_CLOCK,
}

/** Typed error used by the sync layer to report auth failures without retrying. */
class AuthException(message: String, val kind: AuthCause) : Exception(message)

/** Raised when the server rejects a request for reasons other than auth. */
class ServerException(message: String, val status: Int) : Exception(message)

/** Raised when the app is not configured to reach a server (missing/invalid base URL or signing key). */
class ConfigException(message: String) : Exception(message)

/**
 * Thin transport for the ElisDB server push/pull/delete API.
 * Every request carries the ed25519 request signature.
 */
class ApiClient(
  private val baseUrl: String,
  internal val signer: Signer,
  private val client: HttpClient,
  /** Keeps signed timestamps aligned with the server clock; null in tests. */
  private val clock: ClockSync? = null,
) : SyncApi {
  init {
    // Validate every outbound request, including requests made through a client
    // supplied directly to this class in tests. Redirects are disabled in
    // create() so a 3xx cannot bounce to a host outside the policy.
    client.plugin(HttpSend).intercept { request ->
      val urlError = validateServerUrl(request.url.toString())
      if (urlError != null) throw ConfigException("URL de servidor não permitida: $urlError")
      this.execute(request)
    }
  }
  companion object {
    private val json = Json { ignoreUnknownKeys = true }

    /** Detail text the server returns when the signed timestamp is outside its window. */
    private const val STALE_CLOCK_DETAIL = "stale or future timestamp"

    /**
     * Per-request allowances for transfers that legitimately exceed the global
     * 30 s request timeout: a release package or a photo on a slow link.
     */
    private const val RELEASE_DOWNLOAD_TIMEOUT_MS = 10 * 60 * 1000L
    private const val PHOTO_TRANSFER_TIMEOUT_MS = 2 * 60 * 1000L
    private const val TRANSFER_SOCKET_TIMEOUT_MS = 30 * 1000L

    fun create(
      baseUrl: String,
      signer: Signer,
      clock: ClockSync? = null,
    ): ApiClient {
      val http = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        // 401 and other statuses are surfaced to the caller, never auto-retried.
        expectSuccess = false
        // Redirects are never followed: a server must not bounce the client to a
        // host outside the URL policy (defaultRequest validates every URL; a 3xx
        // is surfaced as an error).
        followRedirects = false
        // Bound every request so a stalled connection fails fast instead of
        // leaving the sync stuck in "in progress" indefinitely.
        install(HttpTimeout) {
          connectTimeoutMillis = 15_000
          requestTimeoutMillis = 30_000
          socketTimeoutMillis = 15_000
        }
        // Android's network-security-config (used for cleartext to the local dev
        // host) requires a hostname-aware trust check. The Android engine passes the
        // hostname; CIO does not and would throw CertificateException on HTTPS.
        defaultRequest {
          // Short-circuit the server's allowlist scan by sending our public key,
          // so the server verifies only against this key instead of every one.
          header(AuthHeaders.KEY, signer.publicKeyB64)
        }
      }
      return ApiClient(baseUrl.trimEnd('/'), signer, http, clock)
    }
  }

  /** Sends local records to the server; returns a per-record status. */
  suspend fun push(records: List<RecordDto>, timestamp: Long): List<PushResultDto> =
    push(records, timestamp, null)

  override suspend fun push(
    records: List<RecordDto>,
    timestamp: Long,
    idempotencyKey: String?,
  ): List<PushResultDto> {
    val body = ApiClient.json.encodeToString(PushRequestDto.serializer(), PushRequestDto(records))
    val bodyBytes = body.toByteArray(Charsets.UTF_8)
    val path = "/api/push"
    val response = execute(timestamp) { ts ->
      client.post("$baseUrl$path") {
        contentType(ContentType.Application.Json)
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("POST", path, bodyBytes, ts))
        idempotencyKey?.let { header("Idempotency-Key", it) }
        setBody(bodyBytes)
      }
    }
    if (response.status != HttpStatusCode.OK) {
      throw ServerException("push failed HTTP ${response.status}", response.status.value)
    }
    return response.body()
  }

  /** Pulls the authoritative dataset, including photos (as base64) and tombstones. */
  override suspend fun pull(
    timestamp: Long,
    since: Long?,
    sinceId: String?,
    limit: Int?,
  ): PullResponseDto {
    val path = "/api/pull"
    val query =
      buildString {
        if (since != null) append("?since=$since")
        if (sinceId != null) append(if (since != null) "&" else "?").append("since_id=$sinceId")
        if (limit != null) {
          append(if (since != null || sinceId != null) "&" else "?").append("limit=$limit")
        }
      }
    val url = "$baseUrl$path$query"
    val response = execute(timestamp) { ts ->
      client.get(url) {
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("GET", "$path$query", ByteArray(0), ts))
      }
    }
    if (response.status != HttpStatusCode.OK) throw ServerException("pull failed HTTP ${response.status}", response.status.value)
    return response.body()
  }

  /** Deletes a client (and photos) on the server by id. Returns true when confirmed gone. */
  override suspend fun delete(
    id: String,
    timestamp: Long,
    deletedAt: Long?,
  ): Boolean {
    val path = "/api/clients/$id"
    val query = if (deletedAt != null) "?deleted_at=$deletedAt" else ""
    val url = "$baseUrl$path$query"
    val response = execute(timestamp) { ts ->
      client.delete(url) {
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("DELETE", "$path$query", ByteArray(0), ts))
      }
    }
    return when (response.status) {
      HttpStatusCode.NoContent -> true
      HttpStatusCode.NotFound -> true
      else -> false
    }
  }

  /** Fetches the server's current Unix time (seconds). No authentication required. */
  override suspend fun fetchServerTime(): Long {
    val response = client.get("$baseUrl/api/time")
    if (response.status != io.ktor.http.HttpStatusCode.OK) {
      throw ServerException("time fetch failed HTTP ${response.status}", response.status.value)
    }
    return response.body<TimeDto>().time
  }

  /** Fetches the server's latest data timestamp and how many clients it stores. */
  override suspend fun fetchServerInfo(timestamp: Long): ServerInfo {
    val path = "/api/latest"
    val response = execute(timestamp) { ts ->
      client.get("$baseUrl$path") {
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("GET", path, ByteArray(0), ts))
      }
    }
    if (response.status != HttpStatusCode.OK) {
      throw ServerException("latest fetch failed HTTP ${response.status}", response.status.value)
    }
    val dto = response.body<LatestDto>()
    return ServerInfo(
      latestUpdatedAt = dto.latest_updated_at,
      clientCount = dto.client_count,
      apiVersion = dto.api_version,
      instanceId = dto.instance_id,
      maxPushRecords = dto.max_push_records,
    )
  }

  @kotlinx.serialization.Serializable
  private data class LatestDto(
    val latest_updated_at: Long,
    // Null against an older server that predates the field: the UI then shows
    // a placeholder instead of a misleading 0.
    val client_count: Int? = null,
    @SerialName("api_version") val api_version: Int? = null,
    /** Stable server database identity; absent on older servers. */
    @SerialName("instance_id") val instance_id: String? = null,
    /** Effective push batch limit; absent on older servers. */
    @SerialName("max_push_records") val max_push_records: Int? = null,
  )

  /** Fetches the current client records (metadata only, no photo bytes). */
  override suspend fun fetchClients(timestamp: Long): List<ClienteOutDto> {
    val path = "/api/clients"
    val response = execute(timestamp) { ts ->
      client.get("$baseUrl$path") {
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("GET", path, ByteArray(0), ts))
      }
    }
    if (response.status != HttpStatusCode.OK) {
      throw ServerException("clients fetch failed HTTP ${response.status}", response.status.value)
    }
    return response.body()
  }

  /** Fetches a client's edit history (who created/edited it and when). */
  override suspend fun fetchClientHistory(id: String, timestamp: Long): List<AuditEntryDto> {
    val path = "/api/clients/$id/history"
    val response = execute(timestamp) { ts ->
      client.get("$baseUrl$path") {
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("GET", path, ByteArray(0), ts))
      }
    }
    if (response.status != HttpStatusCode.OK) {
      throw ServerException("history fetch failed HTTP ${response.status}", response.status.value)
    }
    return response.body()
  }

  /** Fetches a page of a client's edit history plus the next cursor. */
  override suspend fun fetchClientHistoryPage(
    id: String,
    limit: Int,
    afterId: Int?,
    timestamp: Long,
  ): HistoryPageDto {
    val query = buildString {
      append("?limit=").append(limit)
      if (afterId != null) append("&after_id=").append(afterId)
    }
    val path = "/api/clients/$id/history$query"
    val response = execute(timestamp) { ts ->
      client.get("$baseUrl$path") {
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("GET", path, ByteArray(0), ts))
      }
    }
    if (response.status != HttpStatusCode.OK) {
      throw ServerException(
        "history page fetch failed HTTP ${response.status}",
        response.status.value,
      )
    }
    return response.body()
  }

  /** Latest published APK, or null when the server has nothing published (404). */
  suspend fun fetchLatestRelease(timestamp: Long): AppReleaseDto? {
    val path = "/api/app/latest"
    val response = execute(timestamp) { ts ->
      client.get("$baseUrl$path") {
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("GET", path, ByteArray(0), ts))
      }
    }
    if (response.status == HttpStatusCode.NotFound) return null
    if (response.status != HttpStatusCode.OK) {
      throw ServerException("release fetch failed HTTP ${response.status}", response.status.value)
    }
    return response.body()
  }

  /**
   * Downloads the published package into [targetDir], reporting progress, and
   * only returns the file when its size and SHA-256 match the published
   * metadata. A corrupted transfer is deleted instead of being offered for
   * installation.
   */
  suspend fun downloadRelease(
    release: AppReleaseDto,
    targetDir: File,
    timestamp: Long,
    onProgress: (received: Long, total: Long?) -> Unit = { _, _ -> },
  ): File {
    val path = "/api/app/download"
    val response = execute(timestamp) { ts ->
      client.get("$baseUrl$path") {
        // A 20 MB package does not fit in the global 30 s request timeout: allow
        // ten minutes for the transfer while keeping the stall detection short.
        timeout {
          requestTimeoutMillis = RELEASE_DOWNLOAD_TIMEOUT_MS
          socketTimeoutMillis = TRANSFER_SOCKET_TIMEOUT_MS
        }
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("GET", path, ByteArray(0), ts))
      }
    }
    if (response.status != HttpStatusCode.OK) {
      throw ServerException("release download failed HTTP ${response.status}", response.status.value)
    }
    targetDir.mkdirs()
    // The filename comes from the server: never let it escape the update
    // directory. Keep only the basename and require an .apk suffix.
    val safeName = release.filename.substringAfterLast('/').substringAfterLast('\\')
    val isPlainName = safeName == release.filename && safeName != "." && safeName != ".."
    if (!isPlainName || !safeName.endsWith(".apk", ignoreCase = true)) {
      throw ServerException("published release filename is not an APK", response.status.value)
    }
    val target = File(targetDir, safeName)
    val digest = MessageDigest.getInstance("SHA-256")
    withContext(Dispatchers.IO) {
      response.bodyAsChannel().toInputStream().use { input ->
        target.outputStream().use { output ->
          val buffer = ByteArray(64 * 1024)
          var received = 0L
          while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            digest.update(buffer, 0, read)
            received += read
            onProgress(received, release.size)
          }
        }
      }
    }
    val sha = digest.digest().joinToString("") { "%02x".format(it) }
    if (target.length() != release.size || !sha.equals(release.sha256, ignoreCase = true)) {
      target.delete()
      throw ReleaseVerificationException("downloaded package does not match the published checksum")
    }
    return target
  }

  @kotlinx.serialization.Serializable
  private data class TimeDto(val time: Long)

  /**
   * Runs a signed request, keeping the clock offset aligned with the server's
   * `Date` header. If the server rejects the timestamp as stale, the offset is
   * corrected from that same response (or `/api/time`) and the request is
   * retried once with a fresh signature, so a wrong device clock cannot turn
   * every sync into a failure.
   */
  private suspend fun execute(
    timestamp: Long,
    send: suspend (Long) -> HttpResponse,
  ): HttpResponse {
    val first = send(timestamp)
    observeServerDate(first)
    if (first.status != HttpStatusCode.Unauthorized) return first
    val detail = first.bodyAsText()
    if (!detail.contains(STALE_CLOCK_DETAIL)) {
      throw AuthException("request rejected with 401: $detail", AuthCause.INVALID_KEY)
    }
    val serverTime = serverTimeFrom(first) ?: runCatching { fetchServerTime() }.getOrNull()
    if (serverTime == null) {
      throw AuthException("request rejected with 401: $detail", AuthCause.STALE_CLOCK)
    }
    val corrected = clock?.observeServerTime(serverTime) ?: serverTime
    val retried = send(corrected)
    observeServerDate(retried)
    if (retried.status == HttpStatusCode.Unauthorized) {
      val retryDetail = retried.bodyAsText()
      val kind =
        if (retryDetail.contains(STALE_CLOCK_DETAIL)) AuthCause.STALE_CLOCK
        else AuthCause.INVALID_KEY
      throw AuthException("request rejected with 401: $retryDetail", kind)
    }
    return retried
  }

  /** Learns the server clock from the response `Date` header (no extra request). */
  private fun observeServerDate(response: HttpResponse) {
    val clock = clock ?: return
    serverTimeFrom(response)?.let { clock.observeServerTime(it) }
  }

  private fun serverTimeFrom(response: HttpResponse): Long? =
    response.headers[HttpHeaders.Date]?.let(::parseHttpDate)

  /** Parses an RFC 1123 date (`Mon, 23 Oct 2026 12:00:00 GMT`) into epoch seconds. */
  private fun parseHttpDate(value: String): Long? =
    runCatching {
        val format =
          SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
            isLenient = true
          }
        format.parse(value)?.time?.div(1000)
      }
      .getOrNull()

  /**
   * Looks up the allowlist identity of the signing key (`GET /api/keys/me`).
   * Informational only: the caller must treat any failure as "no identity".
   * The caller passes a clock-offset-normalized [timestamp] so the signed
   * request is accepted even when the device clock is skewed.
   */
  suspend fun fetchKeyIdentity(timestamp: Long): KeyIdentityDto {
    val path = "/api/keys/me"
    val response = execute(timestamp) { ts ->
      client.get("$baseUrl$path") {
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("GET", path, ByteArray(0), ts))
      }
    }
    if (response.status != HttpStatusCode.OK) {
      throw ServerException("key identity lookup failed HTTP ${response.status}", response.status.value)
    }
    return response.body()
  }

  /** Downloads a photo's raw bytes by id. */
  override suspend fun downloadPhoto(id: String, timestamp: Long): ByteArray {
    val path = "/api/photos/$id"
    val response = execute(timestamp) { ts ->
      client.get("$baseUrl$path") {
        // Photos are routinely megabytes and the global 30 s request timeout is
        // too tight for a slow link; the socket timeout still catches a stall.
        timeout {
          requestTimeoutMillis = PHOTO_TRANSFER_TIMEOUT_MS
          socketTimeoutMillis = TRANSFER_SOCKET_TIMEOUT_MS
        }
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("GET", path, ByteArray(0), ts))
      }
    }
    if (response.status != HttpStatusCode.OK) {
      throw ServerException("photo download failed HTTP ${response.status}", response.status.value)
    }
    return response.bodyAsBytes()
  }

  /** Uploads a photo's raw bytes by id. */
  override suspend fun uploadPhoto(id: String, bytes: ByteArray, timestamp: Long) {
    val path = "/api/photos/$id"
    val response = execute(timestamp) { ts ->
      client.put("$baseUrl$path") {
        // Same allowance as the download: the global 30 s limit is too tight for
        // a multi-megabyte upload on a mobile connection.
        timeout {
          requestTimeoutMillis = PHOTO_TRANSFER_TIMEOUT_MS
          socketTimeoutMillis = TRANSFER_SOCKET_TIMEOUT_MS
        }
        header(AuthHeaders.TIMESTAMP, ts.toString())
        header(AuthHeaders.SIGNATURE, signer.signRequest("PUT", path, bytes, ts))
        setBody(bytes)
      }
    }
    if (response.status != HttpStatusCode.NoContent) {
      throw ServerException("photo upload failed HTTP ${response.status}", response.status.value)
    }
  }

  /** Closes the underlying HTTP client, releasing its connection pool. */
  fun close() {
    client.close()
  }
}
/** The downloaded package does not match the size/checksum the server published. */
class ReleaseVerificationException(message: String) : Exception(message)
