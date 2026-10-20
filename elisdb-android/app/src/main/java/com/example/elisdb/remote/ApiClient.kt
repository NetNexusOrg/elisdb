package com.example.elisdb.remote

import com.example.elisdb.util.AuthHeaders
import com.example.elisdb.util.Signer
import com.example.elisdb.sync.ServerInfo
import com.example.elisdb.sync.SyncApi
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

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
) : SyncApi {
  companion object {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(
      baseUrl: String,
      signer: Signer,
    ): ApiClient {
      val http = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        // 401 and other statuses are surfaced to the caller, never auto-retried.
        expectSuccess = false
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
      return ApiClient(baseUrl.trimEnd('/'), signer, http)
    }
  }

  /** Sends local records to the server; returns a per-record status. */
  override suspend fun push(
    records: List<RecordDto>,
    timestamp: Long,
  ): List<PushResultDto> {
    val body = ApiClient.json.encodeToString(PushRequestDto.serializer(), PushRequestDto(records))
    val bodyBytes = body.toByteArray(Charsets.UTF_8)
    val path = "/api/push"
    val signature = signer.signRequest("POST", path, bodyBytes, timestamp)
    val response = client.post("$baseUrl$path") {
      contentType(ContentType.Application.Json)
      header(AuthHeaders.TIMESTAMP, timestamp.toString())
      header(AuthHeaders.SIGNATURE, signature)
      setBody(bodyBytes)
    }
    checkAuthError(response)
    if (response.status != io.ktor.http.HttpStatusCode.OK) {
      throw ServerException("push failed HTTP ${response.status}", response.status.value)
    }
    return response.body()
  }

  /** Pulls the authoritative dataset, including photos (as base64) and tombstones. */
  override suspend fun pull(
    timestamp: Long,
    since: Long?,
    sinceId: String?,
  ): PullResponseDto {
    val path = "/api/pull"
    val query =
      buildString {
        if (since != null) append("?since=$since")
        if (sinceId != null) append(if (since != null) "&" else "?").append("since_id=$sinceId")
      }
    val url = "$baseUrl$path$query"
    val signature = signer.signRequest("GET", "$path$query", ByteArray(0), timestamp)
    val response = client.get(url) {
      header(AuthHeaders.TIMESTAMP, timestamp.toString())
      header(AuthHeaders.SIGNATURE, signature)
    }
    checkAuthError(response)
    if (response.status != io.ktor.http.HttpStatusCode.OK) throw ServerException("pull failed HTTP ${response.status}", response.status.value)
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
    val signature = signer.signRequest("DELETE", "$path$query", ByteArray(0), timestamp)
    val response = client.delete(url) {
      header(AuthHeaders.TIMESTAMP, timestamp.toString())
      header(AuthHeaders.SIGNATURE, signature)
    }
    checkAuthError(response)
    return when (response.status) {
      io.ktor.http.HttpStatusCode.NoContent -> true
      io.ktor.http.HttpStatusCode.NotFound -> true
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
    val signature = signer.signRequest("GET", path, ByteArray(0), timestamp)
    val response = client.get("$baseUrl$path") {
      header(AuthHeaders.TIMESTAMP, timestamp.toString())
      header(AuthHeaders.SIGNATURE, signature)
    }
    checkAuthError(response)
    if (response.status != io.ktor.http.HttpStatusCode.OK) {
      throw ServerException("latest fetch failed HTTP ${response.status}", response.status.value)
    }
    val dto = response.body<LatestDto>()
    return ServerInfo(latestUpdatedAt = dto.latest_updated_at, clientCount = dto.client_count)
  }

  @kotlinx.serialization.Serializable
  private data class LatestDto(
    val latest_updated_at: Long,
    // Null against an older server that predates the field: the UI then shows
    // a placeholder instead of a misleading 0.
    val client_count: Int? = null,
  )

  /** Fetches a client's edit history (who created/edited it and when). */
  override suspend fun fetchClientHistory(id: String, timestamp: Long): List<AuditEntryDto> {
    val path = "/api/clients/$id/history"
    val signature = signer.signRequest("GET", path, ByteArray(0), timestamp)
    val response = client.get("$baseUrl$path") {
      header(AuthHeaders.TIMESTAMP, timestamp.toString())
      header(AuthHeaders.SIGNATURE, signature)
    }
    checkAuthError(response)
    if (response.status != io.ktor.http.HttpStatusCode.OK) {
      throw ServerException("history fetch failed HTTP ${response.status}", response.status.value)
    }
    return response.body()
  }

  @kotlinx.serialization.Serializable
  private data class TimeDto(val time: Long)

  private suspend fun checkAuthError(response: HttpResponse) {
    if (response.status == io.ktor.http.HttpStatusCode.Unauthorized) {
      val detail = response.bodyAsText()
      val kind =
        if (detail.contains("stale or future timestamp")) AuthCause.STALE_CLOCK
        else AuthCause.INVALID_KEY
      throw AuthException("request rejected with 401: $detail", kind)
    }
  }

  /**
   * Looks up the allowlist identity of the signing key (`GET /api/keys/me`).
   * Informational only: the caller must treat any failure as "no identity".
   * The caller passes a clock-offset-normalized [timestamp] so the signed
   * request is accepted even when the device clock is skewed.
   */
  suspend fun fetchKeyIdentity(timestamp: Long): KeyIdentityDto {
    val path = "/api/keys/me"
    val signature = signer.signRequest("GET", path, ByteArray(0), timestamp)
    val response = client.get("$baseUrl$path") {
      header(AuthHeaders.TIMESTAMP, timestamp.toString())
      header(AuthHeaders.SIGNATURE, signature)
    }
    checkAuthError(response)
    if (response.status != io.ktor.http.HttpStatusCode.OK) {
      throw ServerException("key identity lookup failed HTTP ${response.status}", response.status.value)
    }
    return response.body()
  }

  /** Downloads a photo's raw bytes by id. */
  override suspend fun downloadPhoto(id: String, timestamp: Long): ByteArray {
    val path = "/api/photos/$id"
    val signature = signer.signRequest("GET", path, ByteArray(0), timestamp)
    val response = client.get("$baseUrl$path") {
      header(AuthHeaders.TIMESTAMP, timestamp.toString())
      header(AuthHeaders.SIGNATURE, signature)
    }
    checkAuthError(response)
    if (response.status != io.ktor.http.HttpStatusCode.OK) {
      throw ServerException("photo download failed HTTP ${response.status}", response.status.value)
    }
    return response.bodyAsBytes()
  }

  /** Uploads a photo's raw bytes by id. */
  override suspend fun uploadPhoto(id: String, bytes: ByteArray, timestamp: Long) {
    val path = "/api/photos/$id"
    val signature = signer.signRequest("PUT", path, bytes, timestamp)
    val response = client.put("$baseUrl$path") {
      header(AuthHeaders.TIMESTAMP, timestamp.toString())
      header(AuthHeaders.SIGNATURE, signature)
      setBody(bytes)
    }
    checkAuthError(response)
    if (response.status != io.ktor.http.HttpStatusCode.NoContent) {
      throw ServerException("photo upload failed HTTP ${response.status}", response.status.value)
    }
  }

  /** Closes the underlying HTTP client, releasing its connection pool. */
  fun close() {
    client.close()
  }
}