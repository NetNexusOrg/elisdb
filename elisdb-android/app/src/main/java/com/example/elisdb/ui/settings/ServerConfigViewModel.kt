package com.example.elisdb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.remote.KeyIdentityDto
import com.example.elisdb.settings.SettingsStore
import com.example.elisdb.settings.parseServerConfig
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.util.Signer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Outcome of resolving the user associated with the stored key. */
sealed interface KeyIdentity {
  /** Not yet resolved / no key stored. */
  data object Unresolved : KeyIdentity

  /** Lookup in progress. */
  data object Resolving : KeyIdentity

  /** Server answered; [username] is null when no user is associated. */
  data class Resolved(val username: String?) : KeyIdentity

  /** Lookup failed (network/server error). */
  data object Unavailable : KeyIdentity
}

data class ServerConfigUiState(
  val baseUrl: String = "",
  val signingKey: String = "",
  val hasKey: Boolean = false,
  val signingKeyError: String? = null,
  val keyIdentity: KeyIdentity = KeyIdentity.Unresolved,
)

class ServerConfigViewModel(
  private val settingsStore: SettingsStore,
  private val syncManager: SyncManager,
  private val fetchKeyIdentity: suspend () -> KeyIdentityDto? = { null },
  /**
   * Drops the synchronization state that belongs to the previous server
   * (watermark, photo hashes, clock offset, last sync) before talking to the
   * new one. Records are kept: the full pull that follows reconciles them.
   */
  private val resetSyncState: suspend () -> Unit = {},
) : ViewModel() {
  private val _uiState = MutableStateFlow(ServerConfigUiState())
  val uiState: StateFlow<ServerConfigUiState> = _uiState.asStateFlow()

  init {
    val current = settingsStore.settings()
    _uiState.value =
      ServerConfigUiState(
        baseUrl = current.baseUrl,
        signingKey = current.signingKey,
        hasKey = current.signingKey.isNotEmpty(),
        keyIdentity = importedIdentity(current.userName),
      )
    // The configuration carries the user name, so the server lookup is only a
    // fallback for files issued before it was included.
    if (current.signingKey.isNotEmpty() && current.userName.isBlank()) {
      lookupKeyIdentity()
    }
    viewModelScope.launch {
      syncManager.status.collect { status ->
        if (status == SyncStatus.SUCCESS && _uiState.value.keyIdentity !is KeyIdentity.Resolved) {
          lookupKeyIdentity()
        }
      }
    }
  }

  fun importKey(content: String) {
    _uiState.update { it.copy(signingKeyError = null) }
    val config = parseServerConfig(content)
    if (config == null || !isValidSeed(config.privateSeed)) {
      _uiState.update { current ->
        current.copy(
          signingKeyError = INVALID_CONFIG_MESSAGE,
          hasKey = current.signingKey.isNotEmpty(),
        )
      }
      return
    }
    val baseUrl = config.serverUrl.trim()
    val userName = config.userName.orEmpty()
    settingsStore.save(baseUrl, config.privateSeed, userName)
    _uiState.update {
      it.copy(
        baseUrl = baseUrl,
        signingKey = config.privateSeed,
        hasKey = true,
        keyIdentity = importedIdentity(userName),
      )
    }
    if (userName.isBlank()) lookupKeyIdentity()
    viewModelScope.launch {
      resetSyncState()
      // Only start the full pull after the previous server's cursor is gone, so
      // the new server returns its whole dataset instead of a delta it thinks
      // the app already has.
      syncManager.syncNow()
    }
  }

  fun clearConfig() {
    settingsStore.save("", "", "")
    _uiState.update {
      it.copy(
        baseUrl = "",
        signingKey = "",
        hasKey = false,
        signingKeyError = null,
        keyIdentity = KeyIdentity.Unresolved,
      )
    }
    viewModelScope.launch { resetSyncState() }
  }

  /** The user name stored with the configuration, resolved without a server round trip. */
  private fun importedIdentity(userName: String): KeyIdentity =
    if (userName.isBlank()) KeyIdentity.Unresolved else KeyIdentity.Resolved(userName)

  /** Informational identity lookup: any failure leaves the identity unavailable and never blocks. */
  private fun lookupKeyIdentity() {
    viewModelScope.launch {
      _uiState.update { it.copy(keyIdentity = KeyIdentity.Resolving) }
      val identity = runCatching { fetchKeyIdentity() }.getOrNull()
      _uiState.update {
        it.copy(
          keyIdentity =
            if (identity == null) KeyIdentity.Unavailable else KeyIdentity.Resolved(identity.username),
        )
      }
    }
  }

  private fun isValidSeed(seedB64: String): Boolean =
    try {
      Signer.fromBase64(seedB64)
      true
    } catch (e: IllegalArgumentException) {
      false
    }

  private companion object {
    const val INVALID_CONFIG_MESSAGE =
      "Ficheiro de credencial inválido: deve ser JSON com server_url e uma private_seed ed25519 (32 bytes, base64)."
  }
}
