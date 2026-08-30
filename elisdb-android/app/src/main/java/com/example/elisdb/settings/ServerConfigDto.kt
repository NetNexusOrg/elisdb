package com.example.elisdb.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Server-issued configuration file: the server URL and the ed25519 signing seed. */
@Serializable
data class ServerConfigDto(
  @SerialName("server_url") val serverUrl: String,
  @SerialName("private_seed") val privateSeed: String,
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Parses a server-issued configuration file (JSON with `server_url` and
 * `private_seed`). Returns null when the content is not a valid config file or
 * when the `server_url` uses an unsupported scheme (anything other than `http`
 * or `https`).
 */
fun parseServerConfig(content: String): ServerConfigDto? =
  try {
    val element = json.parseToJsonElement(content).jsonObject
    val serverUrl = element["server_url"]?.jsonPrimitive?.content?.trim().orEmpty()
    val privateSeed = element["private_seed"]?.jsonPrimitive?.content?.trim().orEmpty()
    if (serverUrl.isEmpty() || privateSeed.isEmpty()) return null
    if (!isAllowedServerUrl(serverUrl)) return null
    ServerConfigDto(serverUrl, privateSeed)
  } catch (e: Exception) {
    null
  }

/**
 * A server URL is allowed when it uses the `http` or `https` scheme for any
 * host (a domain or an IP address). Any other scheme is rejected at import
 * time so the user gets a clear error instead of a runtime failure.
 */
private fun isAllowedServerUrl(url: String): Boolean =
  try {
    val u = java.net.URI(url)
    val host = u.host
    u.scheme != null && host != null && host.isNotBlank() &&
      (u.scheme.equals("http", ignoreCase = true) || u.scheme.equals("https", ignoreCase = true))
  } catch (_: Exception) {
    false
  }