package com.example.elisdb.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.remote.ActivityPageDto
import com.example.elisdb.remote.AuditEntryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityLogUiState(
  val isLoading: Boolean = true,
  val error: Boolean = false,
  val entries: List<AuditEntryDto> = emptyList(),
  /** Local files for the photos mentioned by the loaded entries. */
  val photoUris: Map<String, String> = emptyMap(),
  /** Clients among the loaded entries that still exist locally. */
  val existingClienteIds: Set<String> = emptySet(),
  val hasMore: Boolean = false,
  val isLoadingMore: Boolean = false,
)

/**
 * Loads the global activity log, newest first. Each page also resolves the
 * photos it mentions and which clients still exist, so the screen can show
 * thumbnails offline and only navigate to clients that are actually present.
 */
class ActivityLogViewModel(
  private val fetchPage: suspend (Int, Int?) -> ActivityPageDto,
  private val photosByIds: suspend (List<String>) -> Map<String, String> = { emptyMap() },
  private val findExistingClienteIds: suspend (List<String>) -> Set<String> = { emptySet() },
) : ViewModel() {
  private val _uiState = MutableStateFlow(ActivityLogUiState())
  val uiState: StateFlow<ActivityLogUiState> = _uiState.asStateFlow()

  private var nextBeforeId: Int? = null
  private var requestInFlight = false

  init {
    load()
  }

  /** Re-runs the first page after a failure. */
  fun retry() = load()

  /** Appends the next (older) page, when the server supplied a cursor. */
  fun loadMore() {
    val cursor = nextBeforeId ?: return
    if (requestInFlight) return
    requestInFlight = true
    _uiState.update { it.copy(isLoadingMore = true) }
    viewModelScope.launch {
      runCatching { fetchPage(PAGE_SIZE, cursor) }
        .onSuccess { page ->
          nextBeforeId = page.nextBeforeId
          val (photoUris, existingIds) = hydrate(page.entries)
          _uiState.update {
            it.copy(
              isLoadingMore = false,
              entries = it.entries + page.entries,
              photoUris = it.photoUris + photoUris,
              existingClienteIds = it.existingClienteIds + existingIds,
              hasMore = page.nextBeforeId != null,
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
    _uiState.update {
      it.copy(isLoading = true, error = false, entries = emptyList(), hasMore = false)
    }
    viewModelScope.launch {
      runCatching { fetchPage(PAGE_SIZE, null) }
        .onSuccess { page ->
          nextBeforeId = page.nextBeforeId
          val (photoUris, existingIds) = hydrate(page.entries)
          _uiState.update {
            it.copy(
              isLoading = false,
              entries = page.entries,
              photoUris = photoUris,
              existingClienteIds = existingIds,
              hasMore = page.nextBeforeId != null,
              isLoadingMore = false,
            )
          }
        }
        .onFailure {
          nextBeforeId = null
          _uiState.update {
            it.copy(isLoading = false, error = true, entries = emptyList(), hasMore = false)
          }
        }
      requestInFlight = false
    }
  }

  /** Resolves thumbnails and client existence for one page. */
  private suspend fun hydrate(
    entries: List<AuditEntryDto>,
  ): Pair<Map<String, String>, Set<String>> {
    val photoIds = entries.flatMap { it.details?.photos?.added.orEmpty() }.distinct()
    val uris = if (photoIds.isEmpty()) emptyMap() else photosByIds(photoIds)
    val clienteIds =
      entries
        .filter { it.action != "delete" }
        .mapNotNull { it.clienteId }
        .distinct()
    val existing =
      if (clienteIds.isEmpty()) emptySet() else findExistingClienteIds(clienteIds)
    return uris to existing
  }

  private companion object {
    const val PAGE_SIZE = 50
  }
}
