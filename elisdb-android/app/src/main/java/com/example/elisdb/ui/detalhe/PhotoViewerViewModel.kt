package com.example.elisdb.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.Foto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoViewerUiState(
  val fotos: List<Foto> = emptyList(),
  val indice: Int = 0,
)

class PhotoViewerViewModel(
  repository: ClienteRepository,
  clienteId: String,
  initialIndex: Int,
) : ViewModel() {
  private val _uiState = MutableStateFlow(PhotoViewerUiState(indice = initialIndex))
  val uiState: StateFlow<PhotoViewerUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      repository.observeCliente(clienteId).collect { data ->
        val fotos = data?.fotos.orEmpty()
        val indice = _uiState.value.indice.coerceIn(0, (fotos.size - 1).coerceAtLeast(0))
        _uiState.update { it.copy(fotos = fotos, indice = indice) }
      }
    }
  }

  fun setIndice(indice: Int) =
    _uiState.update {
      it.copy(indice = indice.coerceIn(0, (it.fotos.size - 1).coerceAtLeast(0)))
    }
}
