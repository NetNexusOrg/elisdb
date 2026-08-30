package com.example.elisdb.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus

@Composable
internal fun SyncStatusBar(
  syncStatus: SyncStatus,
  syncFault: SyncFault?,
  hasPendingUploads: Boolean = false,
  hasPendingDownloads: Boolean = false,
  hasConfig: Boolean = true,
  onSyncClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val syncing = syncStatus == SyncStatus.IN_PROGRESS
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      syncMessage(syncStatus, syncFault, hasPendingUploads, hasPendingDownloads, hasConfig)?.let { (icon, message) ->
        Icon(
          icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          message,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    IconButton(onClick = onSyncClick, enabled = !syncing, modifier = Modifier.testTag("sync_action")) {
      if (syncing) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
      } else {
        Icon(Icons.Default.Sync, contentDescription = "Sincronizar")
      }
    }
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
    hasPendingUploads && hasPendingDownloads ->
      Icons.Default.CloudUpload to "Sincronização disponível (enviar e receber)"
    hasPendingUploads -> Icons.Default.CloudUpload to "Sincronização disponível (enviar)"
    hasPendingDownloads -> Icons.Default.CloudDownload to "Sincronização disponível (receber)"
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