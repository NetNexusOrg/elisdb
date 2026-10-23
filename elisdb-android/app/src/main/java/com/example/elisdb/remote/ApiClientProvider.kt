package com.example.elisdb.remote

import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.sync.ClockSync
import com.example.elisdb.util.Signer

/**
 * Builds the configured [ApiClient] from runtime settings (with build-time
 * defaults as fallback). Signs with the in-app private key; the matching public
 * key must be registered on the server (see server-auth).
 *
 * Throws [ConfigException] when the server base URL or signing key is missing,
 * so sync can surface an actionable configuration fault before/without a network
 * round trip.
 */
object ApiClientProvider {
  fun create(store: SettingsStore, clock: ClockSync? = null): ApiClient {
    val settings = store.settings()
    if (settings.signingKey.isBlank()) {
      throw ConfigException("Nenhuma chave de assinatura configurada (SIGNER_PRIVATE_KEY_B64)")
    }
    val signer =
      try {
        Signer.fromBase64(settings.signingKey)
      } catch (e: IllegalArgumentException) {
        throw ConfigException("Chave de assinatura inválida")
      }
    if (settings.baseUrl.isBlank()) {
      throw ConfigException("Nenhum servidor configurado (SERVER_BASE_URL)")
    }
    return ApiClient.create(
      baseUrl = settings.baseUrl,
      signer = signer,
      clock = clock,
    )
  }
}