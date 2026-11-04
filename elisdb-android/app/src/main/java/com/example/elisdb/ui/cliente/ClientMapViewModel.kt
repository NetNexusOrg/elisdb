package com.example.elisdb.ui.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.location.Localizacao
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClientMapItem(
  val id: String,
  val nome: String,
  val contrato: String? = null,
  val ativo: Boolean = true,
  val latitude: Double,
  val longitude: Double,
)

enum class ClientMapFilter(val label: String) {
  ALL("Todos os clientes"),
  ACTIVE("Apenas ativos"),
  INACTIVE("Apenas inativos"),
}

sealed interface ClientMapUiState {
  data object Loading : ClientMapUiState

  data class Success(
    val clientes: List<ClientMapItem>,
    val semLocalizacao: Int,
    val totalClientes: Int,
    /** Selected tag, or null when every tag is shown. */
    val tagFiltro: String? = null,
    /** Tags present in the stored clients, uppercase and sorted. */
    val tagsDisponiveis: List<String> = emptyList(),
  ) : ClientMapUiState
}

class ClientMapViewModel(
  repository: ClienteRepository,
  private val locationProvider: suspend () -> Localizacao? = { null },
  private val viewPreferences: ClientViewPreferences = NoOpClientViewPreferences,
) : ViewModel() {
  private val _filtro = MutableStateFlow(viewPreferences.clientMapFilter())
  val filtro: StateFlow<ClientMapFilter> = _filtro.asStateFlow()

  private val _tagFiltro = MutableStateFlow<String?>(null)
  val tagFiltro: StateFlow<String?> = _tagFiltro.asStateFlow()

  private val _myLocation = MutableStateFlow<Localizacao?>(null)
  val myLocation: StateFlow<Localizacao?> = _myLocation.asStateFlow()
  private val _locationResolved = MutableStateFlow(false)
  val locationResolved: StateFlow<Boolean> = _locationResolved.asStateFlow()
  private var locationJob: Job? = null

  fun onFiltroChange(value: ClientMapFilter) {
    _filtro.value = value
    viewPreferences.setClientMapFilter(value)
  }

  fun onTagFiltroChange(value: String?) {
    _tagFiltro.value = value
  }

  /** Refreshes the device location, for example after the permission is granted. */
  fun refreshLocation() {
    if (locationJob?.isActive == true) return
    locationJob =
      viewModelScope.launch {
        try {
          _myLocation.value = locationProvider()
        } finally {
          _locationResolved.value = true
        }
      }
  }

  fun onLocationUnavailable() {
    _locationResolved.value = true
  }

  val uiState: StateFlow<ClientMapUiState> =
    combine(repository.clientes, filtro, tagFiltro) { clientes, filtro, tag ->
        val tagsDisponiveis = clientes.mapNotNull(::normalizedMapTag).distinct().sorted()
        // A tag that no longer exists is treated as "all tags", so the filter
        // never gets stuck showing an empty map.
        val tagSelecionada = tag?.takeIf { it in tagsDisponiveis }
        val porTag =
          if (tagSelecionada == null) clientes
          else clientes.filter { normalizedMapTag(it) == tagSelecionada }
        val comLocalizacao =
          porTag.mapNotNull { cliente ->
            val latitude = cliente.latitude
            val longitude = cliente.longitude
            if (latitude == null || longitude == null || !isValid(latitude, longitude)) {
              null
            } else {
              ClientMapItem(
                id = cliente.id,
                nome = cliente.nome,
                contrato = cliente.contrato,
                ativo = cliente.ativo,
                latitude = latitude,
                longitude = longitude,
              )
            }
          }
        val filtrados =
          when (filtro) {
            ClientMapFilter.ALL -> comLocalizacao
            ClientMapFilter.ACTIVE -> comLocalizacao.filter { it.ativo }
            ClientMapFilter.INACTIVE -> comLocalizacao.filterNot { it.ativo }
          }
        ClientMapUiState.Success(
          clientes =
            filtrados.sortedWith(
              compareBy({ it.nome.uppercase(Locale.ROOT) }, { it.id })
            ),
          // Counted within the tag-filtered set: with a tag filter active, the
          // empty state should talk about the filter rather than "no location".
          semLocalizacao = porTag.size - comLocalizacao.size,
          totalClientes = clientes.size,
          tagFiltro = tagSelecionada,
          tagsDisponiveis = tagsDisponiveis,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientMapUiState.Loading)
}

private fun normalizedMapTag(cliente: Cliente): String? =
  cliente.tag?.trim()?.takeIf { it.isNotEmpty() }?.uppercase(Locale.ROOT)

private fun isValid(latitude: Double, longitude: Double): Boolean =
  latitude in -90.0..90.0 && longitude in -180.0..180.0
