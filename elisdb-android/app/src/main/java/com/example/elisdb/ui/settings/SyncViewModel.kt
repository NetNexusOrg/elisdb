package com.example.elisdb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.sync.ServerAvailability
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.sync.canUseServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUiState(
  val syncNotice: String? = null,
  val syncStatus: SyncStatus = SyncStatus.IDLE,
  val syncFault: SyncFault? = null,
  val hasPendingUploads: Boolean = false,
  val hasPendingDownloads: Boolean = false,
  val totalClientes: Int = 0,
  /** Clients stored on the server; null when unknown (no config, offline, never fetched). */
  val serverClienteCount: Int? = null,
  val pendingUploadCount: Int = 0,
  val lastSuccessAt: Long? = null,
  /** Device clock minus server clock (seconds); negative when the device is behind. */
  val clockSkewSeconds: Long? = null,
  /** True while the user-requested clock correction is running. */
  val clockCorrecting: Boolean = false,
  /** Records preserved because a completed full pull did not find them on the server. */
  val localOnlyCount: Int = 0,
  /** True when the configured server database identity changed. */
  val serverIdentityChanged: Boolean = false,
  /** True while the explicit replace-local action is running. */
  val replacingLocal: Boolean = false,
  /** Shared server reachability, used to disable actions when offline. */
  val serverAvailability: ServerAvailability = ServerAvailability.Online,
)

class SyncViewModel(
  private val syncManager: SyncManager,
  clienteCount: Flow<Int>,
  clockSkewSeconds: Flow<Long?> = flowOf(null),
  private val onCorrectClock: suspend () -> Unit = {},
  serverAvailability: StateFlow<ServerAvailability> = MutableStateFlow(ServerAvailability.Online),
  private val onRefreshServer: suspend () -> Unit = {},
) : ViewModel() {
  private val _uiState =
    MutableStateFlow(
      SyncUiState(
        serverAvailability = serverAvailability.value,
      )
    )
  val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

  private var syncJob: Job? = null

  init {
    viewModelScope.launch {
      clienteCount.collect { count -> _uiState.update { it.copy(totalClientes = count) } }
    }
    viewModelScope.launch {
      syncManager.status.collect { status -> _uiState.update { it.copy(syncStatus = status) } }
    }
    viewModelScope.launch {
      syncManager.fault.collect { fault -> _uiState.update { it.copy(syncFault = fault) } }
    }
    viewModelScope.launch {
      syncManager.lastResult.collect { notice -> _uiState.update { it.copy(syncNotice = notice) } }
    }
    viewModelScope.launch {
      syncManager.hasPendingUploads.collect { pending ->
        _uiState.update { it.copy(hasPendingUploads = pending) }
      }
    }
    viewModelScope.launch {
      syncManager.pendingUploadCount.collect { count ->
        _uiState.update { it.copy(pendingUploadCount = count) }
      }
    }
    viewModelScope.launch {
      syncManager.hasPendingDownloads.collect { pending ->
        _uiState.update { it.copy(hasPendingDownloads = pending) }
      }
    }
    viewModelScope.launch {
      syncManager.serverClienteCount.collect { count ->
        _uiState.update { it.copy(serverClienteCount = count) }
      }
    }
    viewModelScope.launch {
      syncManager.lastSuccessAt.collect { at -> _uiState.update { it.copy(lastSuccessAt = at) } }
    }
    viewModelScope.launch {
      clockSkewSeconds.collect { skew -> _uiState.update { it.copy(clockSkewSeconds = skew) } }
    }
    viewModelScope.launch {
      syncManager.localOnlyCount.collect { count -> _uiState.update { it.copy(localOnlyCount = count) } }
    }
    viewModelScope.launch {
      syncManager.serverIdentityChanged.collect { changed ->
        _uiState.update { it.copy(serverIdentityChanged = changed) }
      }
    }
    viewModelScope.launch {
      serverAvailability.collect { availability ->
        _uiState.update { it.copy(serverAvailability = availability) }
      }
    }
    viewModelScope.launch { onRefreshServer() }
    refreshPendingState()
  }

  /**
   * Refreshes the pending-uploads/downloads indicators and the server client total when the
   * screen opens (best-effort, silent, no data transfer).
   */
  fun refreshPendingState() {
    viewModelScope.launch { syncManager.refreshPendingDownloads() }
  }

  /** Re-measures the clock offset when the user taps "Corrigir" on the skew warning. */
  fun correctClock() {
    if (_uiState.value.clockCorrecting) return
    _uiState.update { it.copy(clockCorrecting = true) }
    viewModelScope.launch {
      try {
        onCorrectClock()
      } finally {
        _uiState.update { it.copy(clockCorrecting = false) }
      }
    }
  }

  fun manualSync() {
    if (!_uiState.value.serverAvailability.canUseServer) return
    if (_uiState.value.syncStatus == SyncStatus.IN_PROGRESS) return
    if (syncJob?.isActive == true) return
    syncJob = viewModelScope.launch { syncManager.syncNow() }
  }

  /**
   * Explicit user-confirmed replacement of local data by the server dataset.
   * The sync manager creates a local snapshot before deleting anything.
   */
  fun replaceLocalWithServer() {
    if (!_uiState.value.serverAvailability.canUseServer) return
    if (_uiState.value.replacingLocal) return
    _uiState.update { it.copy(replacingLocal = true) }
    viewModelScope.launch {
      try {
        syncManager.replaceLocalWithServer()
      } finally {
        _uiState.update { it.copy(replacingLocal = false) }
      }
    }
  }
}
