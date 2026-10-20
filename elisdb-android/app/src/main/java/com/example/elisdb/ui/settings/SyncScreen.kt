package com.example.elisdb.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.ui.components.InfoCard
import com.example.elisdb.ui.components.InfoRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
  viewModel: SyncViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val syncing = state.syncStatus == SyncStatus.IN_PROGRESS
  val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
  val lastSyncLabel =
    state.lastSuccessAt?.let { formatter.format(Date(it * 1000)) } ?: "Nunca"
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Sincronização") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      InfoCard(title = "Estado") {
        SyncStatusBar(
          syncStatus = state.syncStatus,
          syncFault = state.syncFault,
          onCheckClick = viewModel::checkForUpdates,
          checking = state.checking,
          hasPendingUploads = state.hasPendingUploads,
          hasPendingDownloads = state.hasPendingDownloads,
          hasConfig = state.hasKey,
        )
        state.syncNotice?.let { notice ->
          Text(
            notice,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        InfoRow(
          label = "Última sincronização",
          value = lastSyncLabel,
        )
        Button(
          onClick = viewModel::manualSync,
          enabled = !syncing,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(if (syncing) "A sincronizar…" else "Sincronizar")
        }
      }
      InfoCard(title = "Dados") {
        InfoRow(
          label = "Clientes no servidor",
          value = state.serverClienteCount?.toString() ?: "—",
          valueColor =
            if (state.serverClienteCount == null) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
        InfoRow(
          label = "Clientes na app",
          value = state.totalClientes.toString(),
        )
        InfoRow(
          label = "Alterações por enviar",
          value = state.pendingUploadCount.toString(),
        )
        InfoRow(
          label = "Alterações por receber",
          value = if (state.hasPendingDownloads) "≥1" else "0",
          valueColor =
            if (state.hasPendingDownloads) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
