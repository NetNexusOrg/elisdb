package com.example.elisdb.ui.catalogo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elisdb.data.local.CatalogoItem
import com.example.elisdb.data.local.CategoriaCatalogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
  viewModel: CatalogoViewModel,
  onBack: () -> Unit,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val saving by viewModel.saving.collectAsStateWithLifecycle()
  val message by viewModel.message.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var editing by remember { mutableStateOf<CatalogoItem?>(null) }
  var creating by remember { mutableStateOf(false) }

  LaunchedEffect(message) {
    val text = message
    if (text != null && editing == null && !creating) {
      snackbarHostState.showSnackbar(text)
      viewModel.clearMessage()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Catálogo") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
          }
        },
        actions = {
          IconButton(onClick = { viewModel.clearMessage(); creating = true }) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar modelo")
          }
          TextButton(onClick = viewModel::guardar, enabled = !saving) {
            Text("Guardar")
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    if (state.isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
    } else if (state.itens.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          "O catálogo está vazio. Adicione o primeiro modelo.",
          style = MaterialTheme.typography.bodyLarge,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CategoriaCatalogo.entries.forEach { categoria ->
          val itens = state.itens.filter { it.categoria == categoria.slug }
          item(key = "header-${categoria.slug}") {
            Text(
              text = categoria.label,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(top = 8.dp),
            )
          }
          if (itens.isEmpty()) {
            item(key = "empty-${categoria.slug}") {
              Text(
                "Sem modelos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            items(itens, key = { it.id }) { item ->
              CatalogoItemRow(
                item = item,
                onEdit = { viewModel.clearMessage(); editing = item },
                onSetAtivo = { ativo -> viewModel.setAtivo(item.id, ativo) },
              )
            }
          }
        }
      }
    }
  }

  if (creating) {
    CatalogoItemDialog(
      initialNome = "",
      categoria = CategoriaCatalogo.ROEDORES,
      categoryLocked = false,
      saving = saving,
      error = message,
      onDismiss = { creating = false; viewModel.clearMessage() },
      onConfirm = { nome, categoria ->
        viewModel.save(null, nome, categoria) { creating = false }
      },
    )
  }

  editing?.let { item ->
    CatalogoItemDialog(
      initialNome = item.nome,
      categoria = CategoriaCatalogo.fromSlug(item.categoria) ?: CategoriaCatalogo.ROEDORES,
      categoryLocked = true,
      saving = saving,
      error = message,
      onDismiss = { editing = null; viewModel.clearMessage() },
      onConfirm = { nome, categoria ->
        viewModel.save(item.id, nome, categoria) { editing = null }
      },
    )
  }
}

@Composable
private fun CatalogoItemRow(
  item: CatalogoItem,
  onEdit: () -> Unit,
  onSetAtivo: (Boolean) -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.nome, style = MaterialTheme.typography.bodyLarge)
        when {
          !item.ativo ->
            Text(
              "Inativo",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          item.pending ->
            Text(
              "Por sincronizar",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.syncError != null) {
          Text(
            "Erro de sincronização",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
      Box {
        IconButton(onClick = { menuOpen = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "Opções do modelo")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          DropdownMenuItem(
            text = { Text("Editar") },
            onClick = { menuOpen = false; onEdit() },
          )
          DropdownMenuItem(
            text = { Text(if (item.ativo) "Desativar" else "Reativar") },
            onClick = {
              menuOpen = false
              onSetAtivo(!item.ativo)
            },
          )
        }
      }
    }
  }
}

@Composable
private fun CatalogoItemDialog(
  initialNome: String,
  categoria: CategoriaCatalogo,
  categoryLocked: Boolean,
  saving: Boolean,
  error: String?,
  onDismiss: () -> Unit,
  onConfirm: (String, CategoriaCatalogo) -> Unit,
) {
  key(initialNome, categoria.slug, categoryLocked) {
    var nome by remember { mutableStateOf(initialNome) }
    var selected by remember { mutableStateOf(categoria) }
    AlertDialog(
      onDismissRequest = { if (!saving) onDismiss() },
      title = { Text(if (categoryLocked) "Editar modelo" else "Novo modelo") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedTextField(
            value = nome,
            onValueChange = { if (it.length <= 80) nome = it },
            label = { Text("Nome do modelo") },
            singleLine = true,
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
          )
          if (categoryLocked) {
            Text(
              "Categoria: ${selected.label}",
              style = MaterialTheme.typography.bodyMedium,
            )
          } else {
            Text("Categoria", style = MaterialTheme.typography.titleSmall)
            CategoriaCatalogo.entries.forEach { option ->
              FilterChip(
                selected = selected == option,
                onClick = { selected = option },
                label = { Text(option.label) },
              )
            }
          }
          error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
          }
        }
      },
      confirmButton = {
        TextButton(
          enabled = !saving && nome.isNotBlank(),
          onClick = { onConfirm(nome, selected) },
        ) {
          Text("Guardar")
        }
      },
      dismissButton = {
        TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancelar") }
      },
    )
  }
}
