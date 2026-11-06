package com.example.elisdb.ui.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.tagsOf
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal fun matchesQuery(cliente: ClienteComFotos, query: String): Boolean {
  val q = normalizeForSearch(query).trim()
  if (q.isEmpty()) return true
  return listOf(
      cliente.cliente.nome,
      cliente.cliente.contrato,
      cliente.cliente.notas,
    )
    .any { field -> normalizeForSearch(field).contains(q) }
}

private fun normalizeForSearch(value: String?): String {
  if (value == null) return ""
  return Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace("\\p{M}".toRegex(), "")
    .lowercase(Locale.ROOT)
}

/** Canonical tags used for listing and filtering. */
data class ClientListItem(
  val id: String,
  val nome: String,
  val primeiraFotoUri: String?,
  val contrato: String? = null,
  val tags: List<String> = emptyList(),
  val ativo: Boolean = true,
)

/** Which clients the list shows, independent of the search term. */
enum class ClientStatusFilter { ALL, ACTIVE, INACTIVE }

/** How the filtered clients are ordered. */
enum class ClientSortOrder { NAME, TAGS, RECENT, UPDATED }

sealed interface ClientListUiState {
  data object Loading : ClientListUiState

  data class Success(
    val clientes: List<ClientListItem>,
    val query: String = "",
    val filtro: ClientStatusFilter = ClientStatusFilter.ALL,
    val ordem: ClientSortOrder = ClientSortOrder.NAME,
    /** Selected tag, or null when every tag is shown. */
    val tagFiltro: String? = null,
    /** Tags present in the stored clients, uppercase and sorted. */
    val tagsDisponiveis: List<String> = emptyList(),
  ) : ClientListUiState
}

class ClientListViewModel(
  repository: ClienteRepository,
  private val viewPreferences: ClientViewPreferences = NoOpClientViewPreferences,
) : ViewModel() {
  private val repository: ClienteRepository = repository
  private val _query = MutableStateFlow("")
  val query: StateFlow<String> = _query.asStateFlow()

  private val _filtro = MutableStateFlow(viewPreferences.clientListFilter())
  val filtro: StateFlow<ClientStatusFilter> = _filtro.asStateFlow()

  private val _ordem = MutableStateFlow(viewPreferences.clientSortOrder())
  val ordem: StateFlow<ClientSortOrder> = _ordem.asStateFlow()

  private val _tagFiltro = MutableStateFlow<String?>(null)
  val tagFiltro: StateFlow<String?> = _tagFiltro.asStateFlow()

  fun onQueryChange(value: String) {
    _query.value = value
  }

  fun onFiltroChange(value: ClientStatusFilter) {
    _filtro.value = value
    viewPreferences.setClientListFilter(value)
  }

  fun onOrdemChange(value: ClientSortOrder) {
    _ordem.value = value
    viewPreferences.setClientSortOrder(value)
  }

  fun onTagFiltroChange(value: String?) {
    _tagFiltro.value = value
  }

  val uiState: StateFlow<ClientListUiState> =
    combine(repository.clientesComFotos, query, filtro, ordem, tagFiltro) { clientes, q, f, o, tag ->
        val tagsDisponiveis =
          clientes.flatMap { tagsOf(it.cliente.tag) }.distinct().sorted()
        // A tag that no longer exists is treated as "all tags", so the filter
        // never gets stuck showing an empty list.
        val tagSelecionada = tag?.takeIf { it in tagsDisponiveis }
        val byStatus =
          when (f) {
            ClientStatusFilter.ALL -> clientes
            ClientStatusFilter.ACTIVE -> clientes.filter { it.cliente.ativo }
            ClientStatusFilter.INACTIVE -> clientes.filterNot { it.cliente.ativo }
          }
        val byTag =
          if (tagSelecionada == null) byStatus
          else byStatus.filter { tagsOf(it.cliente.tag).contains(tagSelecionada) }
        val filtered = if (q.isBlank()) byTag else byTag.filter { matchesQuery(it, q) }
        // Names are stored uppercase, but sorting here (not only in the DAO query)
        // keeps the order deterministic when the user switches it at runtime.
        val ordered =
          when (o) {
            ClientSortOrder.NAME ->
              filtered.sortedWith(
                compareBy({ it.cliente.nome.uppercase(Locale.ROOT) }, { it.cliente.id })
              )
            ClientSortOrder.TAGS ->
              filtered.sortedWith(
                compareBy<ClienteComFotos> { tagsOf(it.cliente.tag).isEmpty() }
                  .thenBy { tagsOf(it.cliente.tag).firstOrNull().orEmpty() }
                  .thenBy { it.cliente.nome.uppercase(Locale.ROOT) }
                  .thenBy { it.cliente.id }
              )
            ClientSortOrder.RECENT ->
              filtered.sortedWith(
                compareByDescending<ClienteComFotos> { it.cliente.dataCriacao }
                  .thenBy { it.cliente.nome.uppercase(Locale.ROOT) }
                  .thenBy { it.cliente.id }
              )
            ClientSortOrder.UPDATED ->
              filtered.sortedWith(
                compareByDescending<ClienteComFotos> { it.cliente.updatedAt }
                  .thenByDescending { it.cliente.dataCriacao }
                  .thenBy { it.cliente.nome.uppercase(Locale.ROOT) }
                  .thenBy { it.cliente.id }
              )
          }
        ClientListUiState.Success(
          clientes =
            ordered.map {
              ClientListItem(
                id = it.cliente.id,
                nome = it.cliente.nome,
                primeiraFotoUri = it.fotos.firstOrNull()?.uriLocal,
                contrato = it.cliente.contrato,
                tags = tagsOf(it.cliente.tag),
                ativo = it.cliente.ativo,
              )
            },
          query = q,
          filtro = f,
          ordem = o,
          tagFiltro = tagSelecionada,
          tagsDisponiveis = tagsDisponiveis,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientListUiState.Loading)
}