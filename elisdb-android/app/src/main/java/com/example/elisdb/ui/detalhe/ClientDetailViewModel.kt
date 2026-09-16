package com.example.elisdb.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.remote.AuditEntryDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientDetailUiState(
  val isLoading: Boolean = true,
  val cliente: ClienteComFotos? = null,
  val showDeleteConfirmation: Boolean = false,
  val navigateMessage: String? = null,
  val message: String? = null,
  val showLog: Boolean = false,
  val logLoading: Boolean = false,
  val logError: Boolean = false,
  val logEntries: List<AuditEntryDto> = emptyList(),
  val hasPending: Boolean = false,
)

class ClientDetailViewModel(
  private val repository: ClienteRepository,
  clienteId: String,
  private val fetchHistory: suspend (String) -> List<AuditEntryDto> = { emptyList() },
) : ViewModel() {
  private val _uiState =
    MutableStateFlow(ClientDetailUiState(isLoading = true))
  val uiState: StateFlow<ClientDetailUiState> = _uiState.asStateFlow()

  private val _deleted = MutableSharedFlow<Unit>()
  val deleted: SharedFlow<Unit> = _deleted.asSharedFlow()

  init {
    viewModelScope.launch {
      repository.observeCliente(clienteId).collect { data ->
        val pending = data?.cliente?.pending == true || data?.cliente?.deletePending == true
        // The first emission ends loading; a null afterwards means the client
        // is gone (e.g. deleted by sync), not that it is still loading.
        _uiState.update {
          it.copy(isLoading = false, cliente = data, hasPending = pending)
        }
      }
    }
  }

  fun showDeleteConfirmation() =
    _uiState.update { it.copy(showDeleteConfirmation = true) }

  fun dismissDeleteConfirmation() =
    _uiState.update { it.copy(showDeleteConfirmation = false) }

  fun deleteCliente() {
    val cliente = _uiState.value.cliente ?: return
    _uiState.update { it.copy(showDeleteConfirmation = false) }
    viewModelScope.launch {
      runCatching { repository.deleteCliente(cliente.cliente.id) }
        .onSuccess { _deleted.emit(Unit) }
        .onFailure {
          _uiState.update { it.copy(message = "Não foi possível apagar o cliente.") }
        }
    }
  }

  fun openLog() {
    val cliente = _uiState.value.cliente ?: return
    _uiState.update { it.copy(showLog = true, logLoading = true, logError = false, logEntries = emptyList()) }
    viewModelScope.launch {
      runCatching { fetchHistory(cliente.cliente.id) }
        .onSuccess { entries ->
          _uiState.update { it.copy(logLoading = false, logEntries = entries) }
        }
        .onFailure {
          _uiState.update { it.copy(logLoading = false, logError = true) }
        }
    }
  }

  fun dismissLog() =
    _uiState.update { it.copy(showLog = false, logLoading = false, logError = false, logEntries = emptyList()) }

  fun onNavigateMessageShown() = _uiState.update { it.copy(navigateMessage = null) }

  fun onMessageShown() = _uiState.update { it.copy(message = null) }

  fun notifyNavigationUnavailable() =
    _uiState.update {
      it.copy(navigateMessage = "Não foi encontrada uma aplicação de navegação.")
    }
}
