package com.example.elisdb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

class SyncViewModel(
  private val syncManager: SyncManager,
  clienteCount: Flow<Int>,
) : ViewModel() {
  private val _uiState = MutableStateFlow(SyncUiState())
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
    refreshPendingState()
  }

  /**
   * Refreshes the pending-uploads/downloads indicators and the server client total when the
   * screen opens (best-effort, silent, no data transfer).
   */
  fun refreshPendingState() {
    viewModelScope.launch { syncManager.refreshPendingDownloads() }
  }

  fun manualSync() {
    if (_uiState.value.syncStatus == SyncStatus.IN_PROGRESS) return
    if (syncJob?.isActive == true) return
    syncJob = viewModelScope.launch { syncManager.syncNow() }
  }
}
