package com.example.elisdb.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.remote.HistoryPageDto
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
  val hasMore: Boolean = false,
  val isLoadingMore: Boolean = false,
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
  private val fetchHistoryPage: (suspend (String, Int, Int?) -> HistoryPageDto)? = null,
  private val fetchHistory: suspend (String) -> List<AuditEntryDto> = { emptyList() },
) : ViewModel() {
  private val _uiState = MutableStateFlow(ClientLogUiState())
  val uiState: StateFlow<ClientLogUiState> = _uiState.asStateFlow()

  private var nextAfterId: Int? = null
  private var requestInFlight = false

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

  /** Appends the next page, when the server supplied a cursor. */
  fun loadMore() {
    val cursor = nextAfterId ?: return
    val pageFetcher = fetchHistoryPage ?: return
    if (requestInFlight) return
    requestInFlight = true
    _uiState.update { it.copy(isLoadingMore = true) }
    viewModelScope.launch {
      runCatching { pageFetcher(clienteId, PAGE_SIZE, cursor) }
        .onSuccess { page ->
          nextAfterId = page.nextAfterId
          _uiState.update {
            it.copy(
              isLoadingMore = false,
              entries = it.entries + page.entries,
              hasMore = page.nextAfterId != null,
            )
          }
        }
        .onFailure {
          _uiState.update { it.copy(isLoadingMore = false) }
        }
      requestInFlight = false
    }
  }

  private fun load() {
    if (requestInFlight) return
    requestInFlight = true
    _uiState.update { it.copy(isLoading = true, error = false, entries = emptyList(), hasMore = false) }
    viewModelScope.launch {
      runCatching {
        fetchHistoryPage?.invoke(clienteId, PAGE_SIZE, null)
          ?: HistoryPageDto(fetchHistory(clienteId), null)
      }
        .onSuccess { page ->
          nextAfterId = page.nextAfterId
          _uiState.update {
            it.copy(
              isLoading = false,
              entries = page.entries,
              hasMore = page.nextAfterId != null,
              isLoadingMore = false,
            )
          }
        }
        .onFailure {
          nextAfterId = null
          _uiState.update {
            it.copy(isLoading = false, error = true, entries = emptyList(), hasMore = false)
          }
        }
      requestInFlight = false
    }
  }

  private companion object {
    const val PAGE_SIZE = 50
  }
}
