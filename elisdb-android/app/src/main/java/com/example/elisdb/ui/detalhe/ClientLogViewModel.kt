package com.example.elisdb.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.remote.AuditEntryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientLogUiState(
  val isLoading: Boolean = true,
  val error: Boolean = false,
  val entries: List<AuditEntryDto> = emptyList(),
  val clienteNome: String? = null,
  val contrato: String? = null,
  val hasPending: Boolean = false,
  /** Local file of each photo, keyed by id, so entries can show thumbnails offline. */
  val photoUris: Map<String, String> = emptyMap(),
)

/**
 * Loads a client's edit history for the dedicated log page. The client name and
 * contract come from the local database, so the page header is readable even
 * while (or without) the history request completing.
 */
class ClientLogViewModel(
  repository: ClienteRepository,
  private val clienteId: String,
  private val fetchHistory: suspend (String) -> List<AuditEntryDto> = { emptyList() },
) : ViewModel() {
  private val _uiState = MutableStateFlow(ClientLogUiState())
  val uiState: StateFlow<ClientLogUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      repository.observeCliente(clienteId).collect { data ->
        _uiState.update {
          it.copy(
            clienteNome = data?.cliente?.nome,
            contrato = data?.cliente?.contrato,
            hasPending = data?.cliente?.pending == true || data?.cliente?.deletePending == true,
            photoUris = data?.fotos?.associate { foto -> foto.id to foto.uriLocal } ?: emptyMap(),
          )
        }
      }
    }
    load()
  }

  /** Re-runs the history request after a failure. */
  fun retry() = load()

  private fun load() {
    _uiState.update { it.copy(isLoading = true, error = false) }
    viewModelScope.launch {
      runCatching { fetchHistory(clienteId) }
        .onSuccess { entries ->
          _uiState.update { it.copy(isLoading = false, entries = entries) }
        }
        .onFailure {
          _uiState.update { it.copy(isLoading = false, error = true, entries = emptyList()) }
        }
    }
  }
}
