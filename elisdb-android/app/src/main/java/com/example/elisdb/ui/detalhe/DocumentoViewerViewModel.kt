package com.example.elisdb.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.Documento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentoViewerUiState(
  val isLoading: Boolean = true,
  val documento: Documento? = null,
  val fileMissing: Boolean = false,
)

/** Loads one device-map image attachment for the in-app viewer. */
class DocumentoViewerViewModel(
  repository: ClienteRepository,
  clienteId: String,
  documentoId: String,
) : ViewModel() {
  private val _uiState = MutableStateFlow(DocumentoViewerUiState())
  val uiState: StateFlow<DocumentoViewerUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val documento = repository.getCliente(clienteId)?.documentos?.firstOrNull { it.id == documentoId }
      _uiState.update {
        it.copy(
          isLoading = false,
          documento = documento,
          // Report a missing file instead of opening an empty viewer.
          fileMissing = documento != null && !java.io.File(documento.uriLocal).exists(),
        )
      }
    }
  }
}
