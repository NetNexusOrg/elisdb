package com.example.elisdb.ui.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ClientMapItem(
  val id: String,
  val nome: String,
  val contrato: String? = null,
  val ativo: Boolean = true,
  val latitude: Double,
  val longitude: Double,
)

sealed interface ClientMapUiState {
  data object Loading : ClientMapUiState

  data class Success(
    val clientes: List<ClientMapItem>,
    val semLocalizacao: Int,
  ) : ClientMapUiState
}

class ClientMapViewModel(repository: ClienteRepository) : ViewModel() {
  val uiState: StateFlow<ClientMapUiState> =
    repository.clientes
      .map { clientes ->
        val comLocalizacao =
          clientes.mapNotNull { cliente ->
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
        ClientMapUiState.Success(
          clientes =
            comLocalizacao.sortedWith(
              compareBy({ it.nome.uppercase(Locale.ROOT) }, { it.id })
            ),
          semLocalizacao = clientes.size - comLocalizacao.size,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientMapUiState.Loading)
}

private fun isValid(latitude: Double, longitude: Double): Boolean =
  latitude in -90.0..90.0 && longitude in -180.0..180.0
