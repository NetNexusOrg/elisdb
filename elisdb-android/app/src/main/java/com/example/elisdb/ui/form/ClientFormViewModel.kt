package com.example.elisdb.ui.form

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRemovidoException
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ServicoDraft
import com.example.elisdb.data.local.CatalogoItem
import com.example.elisdb.data.local.CategoriaCatalogo
import com.example.elisdb.data.local.ClienteServico
import com.example.elisdb.data.local.Foto
import com.example.elisdb.data.local.MAX_TAGS_FIELD_LENGTH
import com.example.elisdb.data.local.MAX_TAGS_PER_CLIENT
import com.example.elisdb.data.local.MAX_TAG_LENGTH
import com.example.elisdb.data.local.tagsOf
import com.example.elisdb.location.Localizacao
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServicoFormItem(
  val item: CatalogoItem,
  val quantidade: Int? = null,
)

data class ClientFormUiState(
  val isNew: Boolean = true,
  val clienteId: String? = null,
  val isLoading: Boolean = false,
  val nome: String = "",
  val contrato: String = "",
  val tags: List<String> = emptyList(),
  val tagInput: String = "",
  val tagError: String? = null,
  val tagInputVisible: Boolean = false,
  val notas: String = "",
  val ativo: Boolean = true,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val fotos: List<Foto> = emptyList(),
  val catalogo: List<CatalogoItem> = emptyList(),
  val servicos: List<ServicoFormItem> = emptyList(),
  val servicoError: String? = null,
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

  // Associations whose catalog item has not arrived yet are kept invisible but
  // must survive a save; otherwise editing an unrelated field would drop them.
  private var servicosDesconhecidos: List<ClienteServico> = emptyList()

  init {
    if (clienteId != null) {
      loadCliente(clienteId)
    } else {
      viewModelScope.launch {
        _uiState.update { it.copy(catalogo = repository.getCatalogo()) }
      }
    }
  }

  private fun loadCliente(id: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val data = repository.getCliente(id)
      val servicos = repository.getServicos(id)
      val catalogo = repository.getCatalogo()
      servicosDesconhecidos = servicos.mapNotNull { if (it.item == null) it.servico else null }
      _uiState.update {
        it.copy(
          isLoading = false,
          nome = data?.cliente?.nome.orEmpty(),
          contrato = data?.cliente?.contrato.orEmpty(),
          tags = tagsOf(data?.cliente?.tag),
          tagInput = "",
          tagError = null,
          tagInputVisible = false,
          notas = data?.cliente?.notas.orEmpty(),
          ativo = data?.cliente?.ativo ?: true,
          latitude = data?.cliente?.latitude,
          longitude = data?.cliente?.longitude,
          fotos = normalizeProfilePhotos(data?.fotos.orEmpty()),
          catalogo = catalogo,
          servicos =
            servicos.mapNotNull { servico ->
              servico.item?.let { item ->
                ServicoFormItem(item = item, quantidade = servico.servico.quantidade)
              }
            },
        )
      }
    }
  }

  fun onNomeChange(value: String) =
    _uiState.update { it.copy(nome = value.uppercase(Locale.ROOT), nomeError = false) }

  fun onContratoChange(value: String) = _uiState.update { it.copy(contrato = value) }

  fun onStartAddTag() =
    _uiState.update { it.copy(tagInputVisible = true, tagError = null) }

  fun onCancelAddTag() =
    _uiState.update { it.copy(tagInputVisible = false, tagInput = "", tagError = null) }

  fun onTagInputChange(value: String) =
    _uiState.update {
      it.copy(
        tagInput = value.replace(";", "").uppercase(Locale.ROOT).take(MAX_TAG_LENGTH),
        tagError = null,
      )
    }

  fun onAddTag() {
    val state = _uiState.value
    val tag = state.tagInput.trim()
    when {
      tag.isEmpty() -> return
      state.tags.size >= MAX_TAGS_PER_CLIENT ->
        _uiState.update {
          it.copy(tagError = "Máximo de $MAX_TAGS_PER_CLIENT tags por cliente.")
        }
      tag.length > MAX_TAG_LENGTH ->
        _uiState.update {
          it.copy(tagError = "A tag não pode ter mais de $MAX_TAG_LENGTH caracteres.")
        }
      state.tags.any { it.equals(tag, ignoreCase = true) } ->
        _uiState.update {
          it.copy(tagInput = "", tagError = null, tagInputVisible = false)
        }
      else -> {
        val tags = (state.tags + tag).distinct().sorted()
        if (tags.joinToString(";").length > MAX_TAGS_FIELD_LENGTH) {
          _uiState.update {
            it.copy(
              tagError = "O conjunto de tags excede $MAX_TAGS_FIELD_LENGTH caracteres."
            )
          }
        } else {
          _uiState.update {
            it.copy(
              tags = tags,
              tagInput = "",
              tagError = null,
              tagInputVisible = false,
            )
          }
        }
      }
    }
  }

  fun onRemoveTag(tag: String) =
    _uiState.update { it.copy(tags = it.tags - tag, tagError = null) }

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
        .onSuccess { foto ->
          _uiState.update { it.copy(fotos = normalizeProfilePhotos(it.fotos + foto)) }
        }
        .onFailure { _uiState.update { it.copy(message = "Não foi possível guardar a imagem.") } }
    }
  }

  fun onPhotoCaptured(file: File) {
    viewModelScope.launch {
      runCatching { repository.stagePhoto(Uri.fromFile(file)) }
        .onSuccess { foto ->
          _uiState.update {
            it.copy(fotos = normalizeProfilePhotos(it.fotos + foto), showCamera = false)
          }
        }
        .onFailure { _uiState.update { it.copy(showCamera = false, message = "Não foi possível guardar a imagem.") } }
      // stagePhoto re-encodes to a new .webp; the original capture file is no
      // longer referenced, so remove it instead of leaving an orphan on disk.
      runCatching {
        repository.deletePhotoFile(Foto(id = "", clienteId = "", uriLocal = file.absolutePath))
      }
    }
  }

  fun onRemoveFoto(foto: Foto) {
    _uiState.update {
      it.copy(fotos = normalizeProfilePhotos(it.fotos - foto))
    }
    if (foto.id.isEmpty()) {
      viewModelScope.launch { repository.deletePhotoFile(foto) }
    }
  }

  fun onSetProfilePhoto(foto: Foto) {
    _uiState.update { state ->
      state.copy(
        fotos =
          state.fotos.map {
            if (it.uriLocal == foto.uriLocal) it.copy(preferida = true)
            else it.copy(preferida = false)
          }
      )
    }
  }

  fun showCamera() = _uiState.update { it.copy(showCamera = true) }

  fun dismissCamera() = _uiState.update { it.copy(showCamera = false) }

  fun onAddServico(item: CatalogoItem) {
    _uiState.update { state ->
      if (state.servicos.any { it.item.id == item.id }) return@update state
      val quantidade = if (CategoriaCatalogo.fromSlug(item.categoria)?.isDevice == true) 1 else null
      state.copy(
        servicos = state.servicos + ServicoFormItem(item = item, quantidade = quantidade),
        servicoError = null,
      )
    }
  }

  fun onRemoveServico(itemId: String) =
    _uiState.update {
      it.copy(servicos = it.servicos.filterNot { s -> s.item.id == itemId }, servicoError = null)
    }

  fun onQuantidadeChange(itemId: String, value: String) {
    val digits = value.filter { it.isDigit() }.take(4)
    val quantidade = digits.toIntOrNull()
    _uiState.update { state ->
      state.copy(
        servicos =
          state.servicos.map { servico ->
            if (servico.item.id == itemId) servico.copy(quantidade = quantidade) else servico
          },
        servicoError = null,
      )
    }
  }

  fun save() {
    val state = _uiState.value
    if (state.nome.isBlank()) {
      _uiState.update { it.copy(nomeError = true, message = "O nome é obrigatório.") }
      return
    }
    val invalidQuantity =
      state.servicos.any {
        CategoriaCatalogo.fromSlug(it.item.categoria)?.isDevice == true &&
          (it.quantidade == null || it.quantidade !in 1..9999)
      }
    if (invalidQuantity) {
      _uiState.update {
        it.copy(servicoError = "Indique uma quantidade entre 1 e 9999.")
      }
      return
    }
    val servicos =
      state.servicos.map {
        ServicoDraft(
          itemId = it.item.id,
          quantidade =
            if (CategoriaCatalogo.fromSlug(it.item.categoria)?.isDevice == true) it.quantidade
            else null,
        )
      } + servicosDesconhecidos.map { ServicoDraft(it.itemId, it.quantidade) }
    viewModelScope.launch {
      _uiState.update { it.copy(saving = true) }
      runCatching {
        repository.save(
          ClienteDraft(
            id = state.clienteId,
            nome = state.nome,
            contrato = state.contrato,
            tags = state.tags,
            latitude = state.latitude,
            longitude = state.longitude,
            notas = state.notas,
            ativo = state.ativo,
            fotos = state.fotos,
            servicos = servicos,
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
    val orphanPhotos = _uiState.value.fotos.filter { it.id.isEmpty() }
    if (orphanPhotos.isNotEmpty()) {
      CoroutineScope(Dispatchers.IO).launch {
        orphanPhotos.forEach { runCatching { repository.deletePhotoFile(it) } }
      }
    }
  }
}

/**
 * Exactly one photo is the profile photo: the first marked one or the first of
 * the gallery when nothing is marked (photos saved before the flag existed).
 */
internal fun normalizeProfilePhotos(fotos: List<Foto>): List<Foto> {
  if (fotos.isEmpty()) return fotos
  val preferred = fotos.indexOfFirst { it.preferida }.takeIf { it >= 0 } ?: 0
  return fotos.mapIndexed { index, foto ->
    val isPreferred = index == preferred
    if (foto.preferida == isPreferred) foto else foto.copy(preferida = isPreferred)
  }
}
