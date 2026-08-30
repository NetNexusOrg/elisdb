package com.example.elisdb.ui.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.ClienteComFotos
import java.text.Normalizer
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
    .lowercase()
}

data class ClientListItem(
  val id: String,
  val nome: String,
  val primeiraFotoUri: String?,
)

sealed interface ClientListUiState {
  data object Loading : ClientListUiState

  data class Success(
    val clientes: List<ClientListItem>,
    val query: String = "",
  ) : ClientListUiState
}

class ClientListViewModel(repository: ClienteRepository) : ViewModel() {
  private val repository: ClienteRepository = repository
  private val _query = MutableStateFlow("")
  val query: StateFlow<String> = _query.asStateFlow()

  fun onQueryChange(value: String) {
    _query.value = value
  }

  val uiState: StateFlow<ClientListUiState> =
    combine(repository.clientesComFotos, query) { clientes, q ->
        val filtered = if (q.isBlank()) clientes else clientes.filter { matchesQuery(it, q) }
        ClientListUiState.Success(
          clientes =
            filtered.map {
              ClientListItem(
                id = it.cliente.id,
                nome = it.cliente.nome,
                primeiraFotoUri = it.fotos.firstOrNull()?.uriLocal,
              )
            },
          query = q,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientListUiState.Loading)
}