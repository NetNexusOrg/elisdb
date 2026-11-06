package com.example.elisdb.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elisdb.sync.ServerAvailability
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus

/**
 * Sync status summary: a state-tinted icon, a short state title and an optional
 * detail line with the concrete pending work. The pending state is refreshed
 * when the screen opens; there is no separate check action.
 */
@Composable
internal fun SyncStatusBar(
  syncStatus: SyncStatus,
  syncFault: SyncFault?,
  modifier: Modifier = Modifier,
  hasPendingUploads: Boolean = false,
  hasPendingDownloads: Boolean = false,
  serverAvailability: ServerAvailability = ServerAvailability.Online,
  hasSynced: Boolean = true,
  detail: String? = null,
  centered: Boolean = false,
) {
  val message =
    syncMessage(
      syncStatus,
      syncFault,
      hasPendingUploads,
      hasPendingDownloads,
      serverAvailability,
      hasSynced,
    )
  val (containerColor, contentColor) =
    statusColors(
      syncStatus,
      serverAvailability,
      hasPendingUploads,
      hasPendingDownloads,
    )
  if (centered) {
    Column(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      message?.let { (icon, title) ->
        Box(
          modifier = Modifier.size(44.dp).background(containerColor, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
          title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
        detail?.let {
          Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  } else {
    Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      message?.let { (icon, title) ->
        Box(
          modifier = Modifier.size(44.dp).background(containerColor, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          detail?.let {
            Text(
              it,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

/** Icon container colours for the current state, following the theme's colour scheme. */
@Composable
internal fun statusColors(
  syncStatus: SyncStatus,
  serverAvailability: ServerAvailability,
  hasPendingUploads: Boolean,
  hasPendingDownloads: Boolean,
): Pair<Color, Color> {
  val scheme = MaterialTheme.colorScheme
  return when {
    serverAvailability == ServerAvailability.NotConfigured ->
      scheme.surfaceVariant to scheme.onSurfaceVariant
    serverAvailability == ServerAvailability.Offline ||
      serverAvailability == ServerAvailability.ConfigError ->
      scheme.errorContainer to scheme.onErrorContainer
    syncStatus == SyncStatus.IN_PROGRESS ||
      serverAvailability == ServerAvailability.Checking ->
      scheme.tertiaryContainer to scheme.onTertiaryContainer
    // An error is actionable even when there is pending work; do not hide it
    // behind the "pending" state.
    syncStatus == SyncStatus.ERROR -> scheme.errorContainer to scheme.onErrorContainer
    hasPendingUploads || hasPendingDownloads ->
      scheme.tertiaryContainer to scheme.onTertiaryContainer
    else -> scheme.primaryContainer to scheme.onPrimaryContainer
  }
}

internal fun syncMessage(
  syncStatus: SyncStatus,
  syncFault: SyncFault?,
  hasPendingUploads: Boolean = false,
  hasPendingDownloads: Boolean = false,
  serverAvailability: ServerAvailability = ServerAvailability.Online,
  hasSynced: Boolean = true,
): Pair<ImageVector, String>? =
  when {
    serverAvailability == ServerAvailability.NotConfigured ->
      Icons.Default.CloudOff to "Não configurado"
    serverAvailability == ServerAvailability.ConfigError ->
      Icons.Default.CloudOff to "Configuração inválida"
    serverAvailability == ServerAvailability.Offline ->
      Icons.Default.CloudOff to "Indisponível"
    serverAvailability == ServerAvailability.Checking ->
      Icons.Default.Sync to "A verificar…"
    syncStatus == SyncStatus.IN_PROGRESS -> Icons.Default.Sync to "A sincronizar…"
    syncStatus == SyncStatus.ERROR ->
      when (syncFault) {
        SyncFault.CONFIG -> Icons.Default.CloudOff to "Configuração inválida"
        SyncFault.AUTH -> Icons.Default.CloudOff to "Chave inválida"
        SyncFault.CLOCK -> Icons.Default.CloudOff to "Relógio dessincronizado"
        SyncFault.NETWORK -> Icons.Default.CloudOff to "Indisponível"
        null -> Icons.Default.CloudOff to "Indisponível"
      }
    hasPendingUploads && hasPendingDownloads -> Icons.Default.Sync to "Alterações pendentes"
    hasPendingUploads -> Icons.Default.CloudUpload to "Alterações pendentes"
    hasPendingDownloads -> Icons.Default.CloudDownload to "Alterações pendentes"
    syncStatus == SyncStatus.SUCCESS || hasSynced ->
      Icons.Default.CloudDone to "Sincronizado"
    else -> Icons.Default.CloudDone to "Por sincronizar"
  }
