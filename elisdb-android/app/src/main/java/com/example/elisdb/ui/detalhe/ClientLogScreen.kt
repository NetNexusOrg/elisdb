package com.example.elisdb.ui.detalhe

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.elisdb.remote.AuditDetailsDto
import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.remote.AuditFieldDto
import com.example.elisdb.remote.AuditPhotosDto
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.elisdb.data.local.tagsOf

/**
 * Dedicated page for a client's edit history: the log used to live in a small
 * dialog, which made long histories awkward to read. Entries are grouped by
 * day, with one card per change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientLogScreen(
  viewModel: ClientLogViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Log do cliente")
            state.contrato?.takeIf { it.isNotBlank() }?.let { contrato ->
              Text(
                contrato,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
          }
        },
      )
    },
  ) { innerPadding ->
    when {
      state.isLoading ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
          androidx.compose.material3.CircularProgressIndicator()
        }
      state.error -> LogErrorState(
        onRetry = viewModel::retry,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
      state.entries.isEmpty() -> EmptyLog(
        hasPending = state.hasPending,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
      else -> LogTimeline(
        clienteNome = state.clienteNome,
        entries = state.entries,
        photoUris = state.photoUris,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
    }
  }
}

@Composable
private fun LogTimeline(
  clienteNome: String?,
  entries: List<AuditEntryDto>,
  photoUris: Map<String, String>,
  modifier: Modifier = Modifier,
) {
  val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
  val days = remember(entries) { entries.groupBy { dayFormat.format(Date(it.ts * 1000)) } }
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    clienteNome?.let { nome ->
      item { Text(nome, style = MaterialTheme.typography.headlineSmall) }
    }
    item {
      Text(
        text = if (entries.size == 1) "1 alteração" else "${entries.size} alterações",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    days.forEach { (day, dayEntries) ->
      item(key = "dia-$day") {
        Text(
          text = day,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      items(dayEntries) { entry ->
        LogEntryCard(entry = entry, timeFormat = timeFormat, photoUris = photoUris)
      }
    }
  }
}

@Composable
private fun LogEntryCard(
  entry: AuditEntryDto,
  timeFormat: SimpleDateFormat,
  photoUris: Map<String, String>,
) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(36.dp),
      ) {
        Icon(
          imageVector = actionIcon(entry.action),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(8.dp),
        )
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(
              text = entry.username ?: "Utilizador desconhecido",
              style = MaterialTheme.typography.bodyLarge,
            )
            Text(
              text = actionLabel(entry.action),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = timeFormat.format(Date(entry.ts * 1000)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        // Entries recorded before change tracking only describe the actor.
        entry.details?.let { details -> ChangeDetails(details = details, photoUris = photoUris) }
      }
    }
  }
}

/** What an edit changed: one row per field, then a summary of the photos. */
@Composable
private fun ChangeDetails(details: AuditDetailsDto, photoUris: Map<String, String>) {
  if (details.fields.isEmpty() && details.photos == null) return
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    details.fields.forEach { change -> FieldChangeRow(change) }
    details.photos?.let { photos -> PhotoChanges(photos = photos, photoUris = photoUris) }
  }
}

/** "Nome: ~~JOAO~~ → JOAO SILVA", with long values ellipsized. */
@Composable
private fun FieldChangeRow(change: AuditFieldDto) {
  // The status reads better as an action than as "Ativo → Inativo", which would
  // make the reader work out which way round the change went.
  if (change.field == "ativo") {
    Text(
      text = if (change.new == "Ativo") "Ativou o cliente" else "Desativou o cliente",
      style = MaterialTheme.typography.bodySmall,
    )
    return
  }
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = fieldLabel(change.field),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // Tags are always presented uppercased, including historical values.
    val oldText =
      if (change.field == "tag") formatTags(change.old) else change.old ?: "—"
    val newText =
      if (change.field == "tag") formatTags(change.new) else change.new ?: "—"
    Row {
      Text(
        text = oldText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textDecoration = TextDecoration.LineThrough,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false),
      )
      Text(
        text = " → ",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = newText,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

/** Photo changes, with thumbnails from the local copy (the page works offline). */
@Composable
private fun PhotoChanges(photos: AuditPhotosDto, photoUris: Map<String, String>) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    if (photos.added.isNotEmpty()) {
      Text(
        text = photoCount("Adicionou", photos.added.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      val uris = remember(photos.added, photoUris) {
        photos.added.mapNotNull { id -> photoUris[id] }.take(MAX_PHOTO_THUMBNAILS)
      }
      if (uris.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          uris.forEach { uri ->
            AsyncImage(
              model = File(uri),
              contentDescription = "Foto adicionada",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
            )
          }
        }
      }
    }
    if (photos.removed.isNotEmpty()) {
      Text(
        text = photoCount("Removeu", photos.removed.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (photos.reordered) {
      Text(
        text = "Reordenou as fotos",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun photoCount(verb: String, count: Int): String =
  if (count == 1) "$verb 1 foto" else "$verb $count fotos"

private fun formatTags(value: String?): String =
  tagsOf(value).joinToString(", ").ifEmpty { "—" }

private fun fieldLabel(field: String): String =
  when (field) {
    "nome" -> "Nome"
    "contrato" -> "Contrato"
    "tag" -> "Tags"
    "notas" -> "Notas"
    "localizacao" -> "Localização"
    else -> field
  }

/** Beyond four thumbnails the row stops helping and starts crowding the card. */
private const val MAX_PHOTO_THUMBNAILS = 4

@Composable
private fun EmptyLog(hasPending: Boolean, modifier: Modifier = Modifier) {
  val message =
    if (hasPending) {
      "Este cliente tem alterações por sincronizar. O histórico aparece depois de sincronizar."
    } else {
      "Ainda não há alterações registadas no servidor para este cliente."
    }
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.padding(32.dp),
    ) {
      Icon(
        Icons.Filled.History,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(40.dp),
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun LogErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.padding(32.dp),
    ) {
      Icon(
        Icons.Outlined.CloudOff,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(40.dp),
      )
      Text(
        text = "Não foi possível carregar o histórico do cliente.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "Verifica a ligação ao servidor e tenta novamente.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      OutlinedButton(onClick = onRetry) { Text("Tentar novamente") }
    }
  }
}

private fun actionIcon(action: String): ImageVector =
  when (action) {
    "create" -> Icons.Filled.Add
    "update", "edit" -> Icons.Filled.Edit
    else -> Icons.Filled.History
  }

private fun actionLabel(action: String): String =
  when (action) {
    "create" -> "Criou o cliente"
    "update", "edit" -> "Editou o cliente"
    else -> action
  }
