package com.example.elisdb.ui.detalhe

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.ui.map.OsmMap
import java.io.File
import java.util.Date
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
  viewModel: ClientDetailViewModel,
  onBack: () -> Unit,
  onEdit: (String) -> Unit,
  onDeleted: () -> Unit,
  onPhotoClick: (Int) -> Unit,
  onOpenFullscreenMap: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.deleted.collect { onDeleted() }
  }

  LaunchedEffect(state.navigateMessage) {
    state.navigateMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.onNavigateMessageShown()
    }
  }

  LaunchedEffect(state.message) {
    state.message?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.onMessageShown()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          val contrato = state.cliente?.cliente?.contrato
          if (!contrato.isNullOrBlank()) {
            Text(contrato, style = MaterialTheme.typography.titleLarge)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
          }
        },
        actions = {
          val cliente = state.cliente
          if (cliente != null) {
            var menuOpen by remember { mutableStateOf(false) }
            Box {
              IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
              }
              DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                  text = { Text("Editar") },
                  leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                  onClick = {
                    menuOpen = false
                    onEdit(cliente.cliente.id)
                  },
                )
                DropdownMenuItem(
                  text = { Text("Log") },
                  leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                  onClick = {
                    menuOpen = false
                    viewModel.openLog()
                  },
                )
                DropdownMenuItem(
                  text = { Text("Apagar") },
                  leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                  onClick = {
                    menuOpen = false
                    viewModel.showDeleteConfirmation()
                  },
                )
              }
            }
          }
        },
      )
    },
  ) { innerPadding ->
    Box(Modifier.padding(innerPadding).fillMaxSize()) {
      val context = LocalContext.current
      val data = state.cliente
      when {
        state.isLoading || data == null ->
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }

        else ->
          ClientDetailContent(
            data = data,
            onNavigateHere = {
              if (!openInMap(context, data.cliente)) {
                viewModel.notifyNavigationUnavailable()
              }
            },
            onOpenFullscreenMap = { onOpenFullscreenMap(data.cliente.id) },
            onPhotoClick = onPhotoClick,
          )
      }
    }
  }

  if (state.showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = viewModel::dismissDeleteConfirmation,
      title = { Text("Apagar cliente") },
      text = { Text("Tens a certeza de que queres apagar este cliente e as suas fotos?") },
      confirmButton = {
        Button(onClick = viewModel::deleteCliente) { Text("Apagar") }
      },
      dismissButton = {
        OutlinedButton(onClick = viewModel::dismissDeleteConfirmation) { Text("Cancelar") }
      },
    )
  }

  if (state.showLog) {
    AlertDialog(
      onDismissRequest = viewModel::dismissLog,
      title = { Text("Log do cliente") },
      text = {
        when {
          state.logLoading -> CircularProgressIndicator()
          state.logError ->
            Text("Não foi possível carregar o histórico do cliente.")
          state.logEntries.isEmpty() && state.hasPending ->
            Text("Este cliente tem alterações por sincronizar; o histórico aparecerá após a sincronização.")
          state.logEntries.isEmpty() ->
            Text("Não há histórico registado para este cliente.")
          else -> LogEntries(state.logEntries)
        }
      },
      confirmButton = {
        Button(onClick = viewModel::dismissLog) { Text("Fechar") }
      },
    )
  }
}

@Composable
private fun LogEntries(entries: List<AuditEntryDto>) {
  val formatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()) }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    entries.forEach { entry ->
      val actor = entry.username ?: "Utilizador desconhecido"
      val action =
        when (entry.action) {
          "create" -> "Criado por $actor"
          "edit" -> "Editado por $actor"
          else -> entry.action
        }
      val whenText = formatter.format(Date(entry.ts))
      Text("$action — $whenText", style = MaterialTheme.typography.bodyMedium)
    }
  }
}

private fun openInMap(context: android.content.Context, cliente: Cliente): Boolean {
  val latitude = cliente.latitude ?: return false
  val longitude = cliente.longitude ?: return false
  val query = Uri.encode(cliente.nome)
  val uri = "geo:$latitude,$longitude?q=$latitude,$longitude($query)"
  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
  return if (intent.resolveActivity(context.packageManager) != null) {
    try {
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  } else {
    false
  }
}

@Composable
private fun ClientDetailContent(
  data: ClienteComFotos,
  onNavigateHere: () -> Unit,
  onOpenFullscreenMap: () -> Unit,
  onPhotoClick: (Int) -> Unit,
) {
  val cliente = data.cliente

  Column(
    modifier =
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(cliente.nome, style = MaterialTheme.typography.headlineSmall)
    }

    val latitude = cliente.latitude
    val longitude = cliente.longitude
    if (latitude != null && longitude != null) {
      Card(
        onClick = onOpenFullscreenMap,
        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
      ) {
        OsmMap(
          markerPosition = GeoPoint(latitude, longitude),
          draggable = false,
          interactive = false,
          modifier = Modifier.fillMaxSize(),
        )
      }

      Button(onClick = onNavigateHere, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Navigation, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Navegar até aqui")
      }
    } else {
      Text(
        "Sem localização definida.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (!cliente.notas.isNullOrBlank()) {
      Text(cliente.notas, style = MaterialTheme.typography.bodyMedium)
    }

    if (data.fotos.isNotEmpty()) {
      Text("Fotos", style = MaterialTheme.typography.titleMedium)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.fotos.chunked(3).forEachIndexed { rowIndex, row ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            row.forEachIndexed { colIndex, foto ->
              PhotoThumbnail(
                foto = foto,
                onClick = { onPhotoClick(rowIndex * 3 + colIndex) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }

    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun PhotoThumbnail(foto: Foto, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(onClick = onClick, modifier = modifier) {
    AsyncImage(
      model = File(foto.uriLocal),
      contentDescription = "Foto",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)),
    )
  }
}
