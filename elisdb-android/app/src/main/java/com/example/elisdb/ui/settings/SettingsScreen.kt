package com.example.elisdb.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val importKeyLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val text =
          context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        viewModel.importKey(text.orEmpty())
      }
    }
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Definições") },
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
      SyncStatusBar(
        syncStatus = state.syncStatus,
        syncFault = state.syncFault,
        hasPendingUploads = state.hasPendingUploads,
        hasPendingDownloads = state.hasPendingDownloads,
        hasConfig = state.hasKey,
        onSyncClick = viewModel::manualSync,
      )
      state.syncNotice?.let { notice ->
        Text(
          notice,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth(),
        )
      }
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            "Configuração do servidor",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
          )
          ServerInfoRow(
            label = "URL do servidor",
            value = state.baseUrl.ifEmpty { "Não configurado" },
            valueColor =
              if (state.baseUrl.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
              else MaterialTheme.colorScheme.onSurface,
          )
          if (state.signingKeyError != null) {
            Text(
              state.signingKeyError!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.fillMaxWidth(),
            )
          }
          ServerInfoRow(
            label = "Chave de assinatura",
            value = if (state.hasKey) maskedKey(state.signingKey) else "Não configurada",
            valueColor =
              if (state.hasKey) MaterialTheme.colorScheme.onSurface
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (state.hasKey) {
            val identity = state.keyIdentity
            val (userLabel, userColor) =
              when (identity) {
                KeyIdentity.Unresolved, KeyIdentity.Resolving ->
                  "A identificar utilizador…" to MaterialTheme.colorScheme.onSurfaceVariant
                is KeyIdentity.Resolved ->
                  if (identity.username == null) {
                    "Sem utilizador associado a esta chave." to MaterialTheme.colorScheme.onSurfaceVariant
                  } else {
                    identity.username to MaterialTheme.colorScheme.onSurface
                  }
                KeyIdentity.Unavailable ->
                  "Não foi possível determinar o utilizador." to MaterialTheme.colorScheme.onSurfaceVariant
              }
            ServerInfoRow(
              label = "Utilizador associado",
              value = userLabel,
              valueColor = userColor,
            )
            TextButton(
              onClick = viewModel::clearConfig,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Remover configuração")
            }
          }
        }
      }
      OutlinedButton(
        onClick = { importKeyLauncher.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Importar configuração do servidor")
      }
    }
  }
}

@Composable
private fun ServerInfoRow(
  label: String,
  value: String,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      label,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    Text(
      value,
      style = MaterialTheme.typography.bodyLarge,
      color = valueColor,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

private fun maskedKey(seed: String): String {
  if (seed.isEmpty()) return "vazia"
  val prefix = seed.take(4)
  val suffix = seed.takeLast(4)
  val middle = "•".repeat((seed.length - 8).coerceIn(4, 16))
  return "$prefix$middle$suffix"
}