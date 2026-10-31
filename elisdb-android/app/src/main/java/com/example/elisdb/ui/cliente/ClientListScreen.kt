package com.example.elisdb.ui.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun ClientListScreen(
  viewModel: ClientListViewModel,
  onClienteClick: (String) -> Unit,
  onAddClick: () -> Unit,
  modifier: Modifier = Modifier,
  onConfigClick: () -> Unit = {},
  onAboutClick: () -> Unit = {},
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  ClientListScreen(
    state = state,
    onClienteClick = onClienteClick,
    onAddClick = onAddClick,
    onQueryChange = viewModel::onQueryChange,
    onFiltroChange = viewModel::onFiltroChange,
    onOrdemChange = viewModel::onOrdemChange,
    onConfigClick = onConfigClick,
    onAboutClick = onAboutClick,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientListScreen(
  state: ClientListUiState,
  onClienteClick: (String) -> Unit,
  onAddClick: () -> Unit,
  onQueryChange: (String) -> Unit,
  onFiltroChange: (ClientStatusFilter) -> Unit,
  onOrdemChange: (ClientSortOrder) -> Unit,
  onConfigClick: () -> Unit,
  onAboutClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      var menuOpen by remember { mutableStateOf(false) }
      TopAppBar(
        title = { Text("Clientes") },
        actions = {
          IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
          }
          DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
              text = { Text("Novo") },
              onClick = { menuOpen = false; onAddClick() },
              leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
            DropdownMenuItem(
              text = { Text("Configuração") },
              onClick = { menuOpen = false; onConfigClick() },
              leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
            )
            DropdownMenuItem(
              text = { Text("Sobre") },
              onClick = { menuOpen = false; onAboutClick() },
              leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
            )
          }
        },
      )
    },
  ) { innerPadding ->
    when (state) {
      ClientListUiState.Loading ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }

      is ClientListUiState.Success ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Pesquisar clientes…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.query.isNotEmpty()) {
                  IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpar pesquisa")
                  }
                }
                var filtroOpen by remember { mutableStateOf(false) }
                Box {
                  IconButton(onClick = { filtroOpen = true }) {
                    Icon(
                      Icons.Default.Tune,
                      contentDescription = "Filtrar e ordenar",
                      tint =
                        if (state.filtro == ClientStatusFilter.ALL &&
                          state.ordem == ClientSortOrder.NAME
                        ) {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                          MaterialTheme.colorScheme.primary
                        },
                    )
                  }
                  DropdownMenu(expanded = filtroOpen, onDismissRequest = { filtroOpen = false }) {
                    ClientStatusFilter.entries.forEach { option ->
                      DropdownMenuItem(
                        text = { Text(option.label()) },
                        onClick = {
                          filtroOpen = false
                          onFiltroChange(option)
                        },
                        leadingIcon = {
                          if (option == state.filtro) {
                            Icon(Icons.Default.Check, contentDescription = null)
                          } else {
                            Spacer(Modifier.size(24.dp))
                          }
                        },
                      )
                    }
                    HorizontalDivider()
                    ClientSortOrder.entries.forEach { option ->
                      DropdownMenuItem(
                        text = { Text(option.label()) },
                        onClick = {
                          filtroOpen = false
                          onOrdemChange(option)
                        },
                        leadingIcon = {
                          if (option == state.ordem) {
                            Icon(Icons.Default.Check, contentDescription = null)
                          } else {
                            Spacer(Modifier.size(24.dp))
                          }
                        },
                      )
                    }
                  }
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          )
          if (state.clientes.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                when {
                  state.query.isNotBlank() -> "Nenhum cliente corresponde à pesquisa."
                  state.filtro != ClientStatusFilter.ALL -> "Nenhum cliente corresponde ao filtro."
                  else -> "Ainda não há clientes guardados."
                }
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(state.clientes, key = { it.id }) { cliente ->
                ClientListItemRow(cliente, onClick = { onClienteClick(cliente.id) })
              }
            }
          }
        }
    }
  }
}

@Composable
private fun ClientListItemRow(cliente: ClientListItem, onClick: () -> Unit) {  Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val primeiraFoto = cliente.primeiraFotoUri
      Box(
        modifier =
          Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
        if (primeiraFoto != null) {
          AsyncImage(
            model = File(primeiraFoto),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        } else {
          Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp).testTag("generic_avatar"),
          )
        }
      }
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = cliente.nome,
            style = MaterialTheme.typography.titleMedium,
            color =
              if (cliente.ativo) MaterialTheme.colorScheme.onSurface
              else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f).basicMarquee(),
          )
        }
        val contrato = cliente.contrato?.takeIf { it.isNotBlank() }
        if (contrato != null || !cliente.ativo) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (!cliente.ativo) {
              Text(
                text = "Inativo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 6.dp),
              )
            }
            if (contrato != null) {
              Text(
                text = contrato,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

private fun ClientStatusFilter.label(): String =
  when (this) {
    ClientStatusFilter.ALL -> "Todos os clientes"
    ClientStatusFilter.ACTIVE -> "Só clientes ativos"
    ClientStatusFilter.INACTIVE -> "Só clientes inativos"
  }

private fun ClientSortOrder.label(): String =
  when (this) {
    ClientSortOrder.NAME -> "Ordem alfabética"
    ClientSortOrder.RECENT -> "Mais recentes primeiro"
    ClientSortOrder.UPDATED -> "Última edição primeiro"
  }
