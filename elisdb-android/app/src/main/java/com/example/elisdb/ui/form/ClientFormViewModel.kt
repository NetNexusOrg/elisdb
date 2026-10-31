package com.example.elisdb.ui.form

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRemovidoException
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.local.Foto
import com.example.elisdb.location.Localizacao
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientFormUiState(
  val isNew: Boolean = true,
  val clienteId: String? = null,
  val isLoading: Boolean = false,
  val nome: String = "",
  val contrato: String = "",
  val notas: String = "",
  val ativo: Boolean = true,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val fotos: List<Foto> = emptyList(),
  val nomeError: Boolean = false,
  val saving: Boolean = false,
  val showCamera: Boolean = false,
  val message: String? = null,
)

class ClientFormViewModel(
  private val repository: ClienteRepository,
  private val getLocation: suspend () -> Localizacao?,
  clienteId: String?,
) : ViewModel() {
  private val _uiState =
    MutableStateFlow(ClientFormUiState(isNew = clienteId == null, clienteId = clienteId))
  val uiState: StateFlow<ClientFormUiState> = _uiState.asStateFlow()

  // Buffered so an event emitted while the screen is being composed is not lost
  // (a suspending emit with no collector left `saving` stuck at true).
  private val _saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
  val saved: SharedFlow<String> = _saved.asSharedFlow()

  private var wasSaved = false

  init {
    if (clienteId != null) {
      loadCliente(clienteId)
    }
  }

  private fun loadCliente(id: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val data = repository.getCliente(id)
      _uiState.update {
        it.copy(
          isLoading = false,
          nome = data?.cliente?.nome.orEmpty(),
          contrato = data?.cliente?.contrato.orEmpty(),
          notas = data?.cliente?.notas.orEmpty(),
          ativo = data?.cliente?.ativo ?: true,
          latitude = data?.cliente?.latitude,
          longitude = data?.cliente?.longitude,
          fotos = data?.fotos.orEmpty(),
        )
      }
    }
  }

  fun onNomeChange(value: String) =
    _uiState.update { it.copy(nome = value.uppercase(Locale.ROOT), nomeError = false) }

  fun onContratoChange(value: String) = _uiState.update { it.copy(contrato = value) }

  fun onNotasChange(value: String) = _uiState.update { it.copy(notas = value) }

  fun onAtivoChange(value: Boolean) = _uiState.update { it.copy(ativo = value) }

  fun onLocationChange(latitude: Double, longitude: Double) =
    _uiState.update { it.copy(latitude = latitude, longitude = longitude) }

  fun useCurrentLocation() {
    viewModelScope.launch {
      val location = getLocation()
      if (location != null) {
        _uiState.update {
          it.copy(latitude = location.latitude, longitude = location.longitude, message = null)
        }
      } else {
        _uiState.update { it.copy(message = "Não foi possível obter a localização atual.") }
      }
    }
  }

  fun createPhotoFile(): File = repository.createPhotoFile()

  fun onPhotoPicked(uri: Uri) {
    viewModelScope.launch {
      runCatching { repository.stagePhoto(uri) }
        .onSuccess { foto -> _uiState.update { it.copy(fotos = it.fotos + foto) } }
        .onFailure { _uiState.update { it.copy(message = "Não foi possível guardar a imagem.") } }
    }
  }

  fun onPhotoCaptured(file: File) {
    viewModelScope.launch {
      runCatching { repository.stagePhoto(Uri.fromFile(file)) }
        .onSuccess { foto -> _uiState.update { it.copy(fotos = it.fotos + foto, showCamera = false) } }
        .onFailure { _uiState.update { it.copy(showCamera = false, message = "Não foi possível guardar a imagem.") } }
      // stagePhoto re-encodes to a new .webp; the original capture file is no
      // longer referenced, so remove it instead of leaving an orphan on disk.
      runCatching { withContext(Dispatchers.IO) { file.delete() } }
    }
  }

  fun onRemoveFoto(foto: Foto) {
    _uiState.update { it.copy(fotos = it.fotos - foto) }
    if (foto.id.isEmpty()) {
      viewModelScope.launch { repository.deletePhotoFile(foto) }
    }
  }

  fun showCamera() = _uiState.update { it.copy(showCamera = true) }

  fun dismissCamera() = _uiState.update { it.copy(showCamera = false) }

  fun save() {
    val state = _uiState.value
    if (state.nome.isBlank()) {
      _uiState.update { it.copy(nomeError = true, message = "O nome é obrigatório.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(saving = true) }
      runCatching {
        repository.save(
          ClienteDraft(
            id = state.clienteId,
            nome = state.nome,
            contrato = state.contrato,
            latitude = state.latitude,
            longitude = state.longitude,
            notas = state.notas,
            ativo = state.ativo,
            fotos = state.fotos,
          )
        )
      }
        .onSuccess { id ->
          wasSaved = true
          // Saving is a user-triggered sync point: send the changes in the
          // background while the form closes. SyncManager runs the transfer in
          // its own scope, so navigation (and this view model's clear) does not
          // cancel it, and a failure surfaces through the list screen's sync
          // state instead of a snackbar on a form that is already gone.
          viewModelScope.launch { runCatching { repository.syncNow() } }
          _saved.tryEmit(id)
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              saving = false,
              message =
                if (error is ClienteRemovidoException) {
                  "Este cliente foi eliminado entretanto. As alterações não foram guardadas."
                } else {
                  "Não foi possível guardar o cliente."
                },
            )
          }
        }
    }
  }

  fun onMessageShown() = _uiState.update { it.copy(message = null) }

  override fun onCleared() {
    if (wasSaved) return
    val orphanFiles = _uiState.value.fotos.filter { it.id.isEmpty() }.map { File(it.uriLocal) }
    if (orphanFiles.isNotEmpty()) {
      CoroutineScope(Dispatchers.IO).launch { orphanFiles.forEach { runCatching { it.delete() } } }
    }
  }
}
