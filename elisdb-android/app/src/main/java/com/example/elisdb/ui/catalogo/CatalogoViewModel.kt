package com.example.elisdb.ui.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.CatalogoCategoriaImutavelException
import com.example.elisdb.data.CatalogoItemRemovidoException
import com.example.elisdb.data.CatalogoNomeDuplicadoException
import com.example.elisdb.data.CatalogoNomeInvalidoException
import com.example.elisdb.data.local.CatalogoItem
import com.example.elisdb.data.local.CategoriaCatalogo
import com.example.elisdb.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogoUiState(
  val itens: List<CatalogoItem> = emptyList(),
  val isLoading: Boolean = true,
)

class CatalogoViewModel(private val repository: ClienteRepository) : ViewModel() {
  private val _saving = MutableStateFlow(false)
  val saving: StateFlow<Boolean> = _saving

  private val _message = MutableStateFlow<String?>(null)
  val message: StateFlow<String?> = _message

  val uiState: StateFlow<CatalogoUiState> =
    combine(repository.catalogo, _saving) { itens, _ ->
        val ordered =
          itens.sortedWith(
            compareBy(
              { CategoriaCatalogo.fromSlug(it.categoria)?.ordinal ?: Int.MAX_VALUE },
              { com.example.elisdb.data.normalizarNomeCatalogo(it.nome) },
            )
          )
        CatalogoUiState(itens = ordered, isLoading = false)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogoUiState())

  fun save(
    id: String?,
    nome: String,
    categoria: CategoriaCatalogo,
    onSuccess: () -> Unit,
  ) {
    if (_saving.value) return
    _saving.value = true
    viewModelScope.launch {
      try {
        repository.saveCatalogoItem(id, nome, categoria)
        _message.value = null
        onSuccess()
      } catch (e: CatalogoNomeDuplicadoException) {
        _message.value = "Já existe um modelo com esse nome nesta categoria."
      } catch (e: CatalogoNomeInvalidoException) {
        _message.value = "O nome do modelo é obrigatório."
      } catch (e: CatalogoCategoriaImutavelException) {
        _message.value = "A categoria não pode ser alterada."
      } catch (e: CatalogoItemRemovidoException) {
        _message.value = "Este modelo já não existe."
      } finally {
        _saving.value = false
      }
    }
  }

  fun setAtivo(id: String, ativo: Boolean) {
    viewModelScope.launch {
      try {
        repository.setCatalogoAtivo(id, ativo)
      } catch (e: CatalogoItemRemovidoException) {
        _message.value = "Este modelo já não existe."
      }
    }
  }

  /** Persists any pending catalog changes to the server. */
  fun guardar() {
    if (_saving.value) return
    _saving.value = true
    viewModelScope.launch {
      try {
        val status = repository.syncNow()
        val pending = repository.getCatalogo().any { it.pending }
        _message.value =
          when {
            status == SyncStatus.ERROR -> "Não foi possível sincronizar o catálogo."
            pending -> "Catálogo guardado, mas há itens por sincronizar."
            else -> "Catálogo guardado."
          }
      } catch (e: Exception) {
        _message.value = "Não foi possível sincronizar o catálogo."
      } finally {
        _saving.value = false
      }
    }
  }

  fun clearMessage() {
    _message.value = null
  }
}
