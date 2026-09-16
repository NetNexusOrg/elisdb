package com.example.elisdb.ui.cliente

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
  onSettingsClick: () -> Unit = {},
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  ClientListScreen(
    state = state,
    onClienteClick = onClienteClick,
    onAddClick = onAddClick,
    onQueryChange = viewModel::onQueryChange,
    onSettingsClick = onSettingsClick,
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
  onSettingsClick: () -> Unit,
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
              text = { Text("Definições") },
              onClick = { menuOpen = false; onSettingsClick() },
              leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
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
              if (state.query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                  Icon(Icons.Default.Clear, contentDescription = "Limpar pesquisa")
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
                if (state.query.isBlank()) {
                  "Ainda não há clientes guardados."
                } else {
                  "Nenhum cliente corresponde à pesquisa."
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
private fun ClientListItemRow(cliente: ClientListItem, onClick: () -> Unit) {
  Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
      Text(cliente.nome, style = MaterialTheme.typography.titleMedium)
    }
  }
}
