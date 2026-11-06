package com.example.elisdb.sync

/**
 * Whether the app can talk to the configured server.
 *
 * This is deliberately separate from [SyncFault]: a missing credential and an
 * unreachable server are different problems and must not both read as
 * "Não configurado".
 */
sealed interface ServerAvailability {
  /** No server URL or signing key is stored. */
  data object NotConfigured : ServerAvailability

  /** A credential is stored, but it is not valid (bad seed or URL). */
  data object ConfigError : ServerAvailability

  /** A probe is in flight; actions that need the server stay disabled. */
  data object Checking : ServerAvailability

  /** The server responded, even if it answered with an HTTP error. */
  data object Online : ServerAvailability

  /** No transport-level response: DNS, connect, timeout or network loss. */
  data object Offline : ServerAvailability
}

/** True only when the app may start a network operation. */
val ServerAvailability.canUseServer: Boolean
  get() = this == ServerAvailability.Online
