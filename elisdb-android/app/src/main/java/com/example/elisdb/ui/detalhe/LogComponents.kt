package com.example.elisdb.ui.detalhe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import com.example.elisdb.data.local.tagsOf
import com.example.elisdb.remote.AuditDetailsDto
import com.example.elisdb.remote.AuditEntryDto
import com.example.elisdb.remote.AuditFieldDto
import com.example.elisdb.remote.AuditPhotosDto
import java.io.File
import java.text.SimpleDateFormat

/**
 * One audit entry card, shared by the per-client log and the global activity
 * log. The global page also shows the client name, which is clickable when the
 * client still exists.
 */
@Composable
internal fun LogEntryCard(
  entry: AuditEntryDto,
  timeFormat: SimpleDateFormat,
  photoUris: Map<String, String>,
  clienteNome: String? = null,
  onClienteClick: (() -> Unit)? = null,
) {
  Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
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
        if (clienteNome != null) {
          Text(
            text = clienteNome,
            style = MaterialTheme.typography.titleSmall,
            color =
              if (onClienteClick != null) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
              if (onClienteClick != null) Modifier.clickable(onClick = onClienteClick)
              else Modifier,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(
              text = entry.username ?: "Utilizador desconhecido",
              style = MaterialTheme.typography.bodyLarge,
            )
            Text(
              text = actionLabel(entry.action, entry.kind),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = timeFormat.format(java.util.Date(entry.ts * 1000)),
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
  if (details.fields.isEmpty() && details.photos == null && details.services.isEmpty()) return
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    details.fields.forEach { change -> FieldChangeRow(change) }
    details.services.forEach { service ->
      Text(
        text =
          when (service.change) {
            "added" ->
              if (service.new != null) "Adicionou ${service.nome} (qtd. ${service.new})"
              else "Adicionou ${service.nome}"
            "removed" -> "Removeu ${service.nome}"
            "quantity" -> "${service.nome}: ${service.old ?: "—"} → ${service.new ?: "—"}"
            else -> service.nome
          },
        style = MaterialTheme.typography.bodySmall,
      )
    }
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

/** Empty state shared by the logs; the message explains what is missing. */
@Composable
internal fun LogEmptyState(message: String, modifier: Modifier = Modifier) {
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

/** Error state shared by the logs, with a retry action. */
@Composable
internal fun LogErrorState(
  message: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
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
        text = message,
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

internal fun actionIcon(action: String): ImageVector =
  when (action) {
    "create" -> Icons.Filled.Add
    "update", "edit" -> Icons.Filled.Edit
    "delete" -> Icons.Filled.Delete
    else -> Icons.Filled.History
  }

internal fun actionLabel(action: String, kind: String = "client"): String {
  if (kind == "catalog") {
    return when (action) {
      "create" -> "Criou o modelo"
      "update", "edit" -> "Editou o modelo"
      "deactivate" -> "Desativou o modelo"
      "reactivate" -> "Reativou o modelo"
      else -> action
    }
  }
  return when (action) {
    "create" -> "Criou o cliente"
    "update", "edit" -> "Editou o cliente"
    "delete" -> "Apagou o cliente"
    "deactivate" -> "Desativou o cliente"
    "reactivate" -> "Ativou o cliente"
    else -> action
  }
}
