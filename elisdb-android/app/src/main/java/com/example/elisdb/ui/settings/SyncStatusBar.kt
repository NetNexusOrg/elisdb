package com.example.elisdb.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
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
  hasConfig: Boolean = true,
  detail: String? = null,
) {
  val message =
    syncMessage(syncStatus, syncFault, hasPendingUploads, hasPendingDownloads, hasConfig)
  val (containerColor, contentColor) =
    statusColors(syncStatus, hasConfig, hasPendingUploads, hasPendingDownloads)
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

/** Icon container colours for the current state, following the theme's colour scheme. */
@Composable
internal fun statusColors(
  syncStatus: SyncStatus,
  hasConfig: Boolean,
  hasPendingUploads: Boolean,
  hasPendingDownloads: Boolean,
): Pair<Color, Color> {
  val scheme = MaterialTheme.colorScheme
  return when {
    syncStatus == SyncStatus.IN_PROGRESS -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    !hasConfig -> scheme.surfaceVariant to scheme.onSurfaceVariant
    hasPendingUploads || hasPendingDownloads ->
      scheme.tertiaryContainer to scheme.onTertiaryContainer
    syncStatus == SyncStatus.ERROR -> scheme.errorContainer to scheme.onErrorContainer
    else -> scheme.primaryContainer to scheme.onPrimaryContainer
  }
}

internal fun syncMessage(
  syncStatus: SyncStatus,
  syncFault: SyncFault?,
  hasPendingUploads: Boolean = false,
  hasPendingDownloads: Boolean = false,
  hasConfig: Boolean = true,
): Pair<ImageVector, String>? =
  when {
    syncStatus == SyncStatus.IN_PROGRESS -> Icons.Default.Sync to "A sincronizar…"
    !hasConfig -> Icons.Default.CloudOff to "Não configurado"
    hasPendingUploads && hasPendingDownloads -> Icons.Default.Sync to "Alterações por sincronizar"
    hasPendingUploads -> Icons.Default.CloudUpload to "Alterações por sincronizar"
    hasPendingDownloads -> Icons.Default.CloudDownload to "Alterações por sincronizar"
    syncStatus == SyncStatus.SUCCESS -> Icons.Default.CloudDone to "Sincronizado"
    syncStatus == SyncStatus.ERROR ->
      when (syncFault) {
        SyncFault.CONFIG -> Icons.Default.CloudOff to "Não configurado"
        SyncFault.AUTH -> Icons.Default.CloudOff to "Servidor rejeitou a sincronização (chave não registada)."
        SyncFault.CLOCK -> Icons.Default.CloudOff to "Relógio do dispositivo dessincronizado. Verifique a hora do dispositivo."
        SyncFault.NETWORK -> Icons.Default.CloudOff to "Falha na sincronização. Verifique a ligação."
        null -> Icons.Default.CloudOff to "Falha na sincronização."
      }
    else -> Icons.Default.CloudDone to "Sincronizado"
  }
