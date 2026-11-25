package com.example.elisdb.ui.detalhe

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.ClienteServicoComItem
import com.example.elisdb.data.local.CategoriaCatalogo
import com.example.elisdb.data.local.Foto
import com.example.elisdb.data.local.tagsOf
import com.example.elisdb.ui.components.DecisionDialog
import com.example.elisdb.ui.map.ElisMap
import java.io.File
import org.maplibre.android.geometry.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
  viewModel: ClientDetailViewModel,
  onBack: () -> Unit,
  onEdit: (String) -> Unit,
  onViewLog: (String) -> Unit,
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
          val cliente = state.cliente?.cliente
          if (cliente != null) {
            val contrato = cliente.contrato
            if (!contrato.isNullOrBlank()) {
              Text(contrato, style = MaterialTheme.typography.titleLarge)
            }
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
                Icon(Icons.Default.Menu, contentDescription = "Mais opções")
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
                    onViewLog(cliente.cliente.id)
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
        state.isLoading ->
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }

        data == null ->
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cliente não encontrado.", style = MaterialTheme.typography.titleMedium)
          }

        else ->
          ClientDetailContent(
            data = data,
            servicos = state.servicos,
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
    DecisionDialog(
      title = "Apagar cliente",
      text = "Tens a certeza de que queres apagar este cliente e as suas fotos?",
      confirmLabel = "Apagar",
      confirmColor = MaterialTheme.colorScheme.error,
      onConfirm = viewModel::deleteCliente,
      onDismissRequest = viewModel::dismissDeleteConfirmation,
    )
  }
}

private fun openInMap(context: android.content.Context, cliente: Cliente): Boolean {
  val latitude = cliente.latitude ?: return false
  val longitude = cliente.longitude ?: return false
  val query = Uri.encode(cliente.nome)
  val uri = "geo:$latitude,$longitude?q=$latitude,$longitude($query)"
  val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientDetailContent(
  data: ClienteComFotos,
  servicos: List<ClienteServicoComItem>,
  onNavigateHere: () -> Unit,
  onOpenFullscreenMap: () -> Unit,
  onPhotoClick: (Int) -> Unit,
) {
  val cliente = data.cliente

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          cliente.nome,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f, fill = false),
        )
        if (!cliente.ativo) {
          StatusPill(
            text = "Inativo",
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
        if (cliente.localOnly) {
          StatusPill(
            text = "Apenas local",
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }

      val tags = tagsOf(cliente.tag)
      if (tags.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          tags.forEach { tag ->
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = tag,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      SectionTitle("Localização")
      val latitude = cliente.latitude
      val longitude = cliente.longitude
      if (latitude != null && longitude != null) {
        Card(
          onClick = onOpenFullscreenMap,
          modifier =
            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
        ) {
          ElisMap(
            markerPosition = LatLng(latitude, longitude),
            draggable = false,
            interactive = false,
            modifier = Modifier.fillMaxSize(),
          )
        }
        OutlinedButton(onClick = onNavigateHere, modifier = Modifier.fillMaxWidth()) {
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
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      SectionTitle("Notas")
      if (cliente.notas.isNullOrBlank()) {
        Text(
          "Sem notas.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          SelectionContainer {
            Text(
              cliente.notas,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      }
    }

    ServicosSection(servicos)

    if (data.fotos.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Fotos")
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
    }

    Spacer(Modifier.height(8.dp))
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
  )
}

@Composable
private fun StatusPill(
  text: String,
  container: androidx.compose.ui.graphics.Color,
  content: androidx.compose.ui.graphics.Color,
) {
  Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
    Text(
      text,
      style = MaterialTheme.typography.labelLarge,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServicosSection(servicos: List<ClienteServicoComItem>) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle("Aparelhos e Tratamentos")
    val conhecidos =
      servicos.mapNotNull { s -> s.item?.let { item -> item to s.servico.quantidade } }
    if (conhecidos.isEmpty()) {
      Text(
        "Sem aparelhos ou tratamentos.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      return@Column
    }
    CategoriaCatalogo.entries.forEach { categoria ->
      val grupo =
        conhecidos
          .filter { (item, _) -> item.categoria == categoria.slug }
          .sortedBy { (item, _) -> com.example.elisdb.data.normalizarNomeCatalogo(item.nome) }
      if (grupo.isEmpty()) return@forEach
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            categoria.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            if (categoria.isDevice) {
              grupo.sumOf { (_, quantidade) -> quantidade ?: 0 }.toString()
            } else {
              grupo.size.toString()
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          grupo.forEach { (item, quantidade) ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              shape = RoundedCornerShape(50),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
              ) {
                Text(
                  text = item.nome,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                if (categoria.isDevice) {
                  Text(
                    text = (quantidade ?: 0).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
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
