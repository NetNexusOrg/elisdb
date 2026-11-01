package com.example.elisdb.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.ui.components.InfoCard
import com.example.elisdb.ui.components.StatTile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single settings screen: sync status and the server configuration live together,
 * so the state, the data counts and the connection details are on one page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
  syncViewModel: SyncViewModel,
  serverViewModel: ServerConfigViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by syncViewModel.uiState.collectAsStateWithLifecycle()
  val serverState by serverViewModel.uiState.collectAsStateWithLifecycle()
  val configured = serverState.hasKey
  val syncing = state.syncStatus == SyncStatus.IN_PROGRESS
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showClearConfigDialog by remember { mutableStateOf(false) }
  val importConfigLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        scope.launch {
          val text =
            withContext(Dispatchers.IO) {
              context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
          serverViewModel.importKey(text.orEmpty())
        }
      }
    }
  val importConfig: () -> Unit = {
    importConfigLauncher.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
  }
  val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
  val lastSync =
    state.lastSuccessAt?.let { formatter.format(Date(it * 1000)) } ?: "Ainda sem sincronizações"
  val pendingDetail =
    when {
      !configured -> "Importa a credencial do utilizador para sincronizar."
      state.hasPendingUploads && state.hasPendingDownloads ->
        "${state.pendingUploadCount} por enviar · novidades por receber"
      state.hasPendingUploads -> "${state.pendingUploadCount} por enviar"
      state.hasPendingDownloads -> "Novidades por receber"
      else -> null
    }
  val (chipContainer, chipContent) =
    statusColors(
      state.syncStatus,
      configured,
      state.hasPendingUploads,
      state.hasPendingDownloads,
    )
  val chipLabel =
    when {
      !configured -> "Por configurar"
      syncing -> "A sincronizar"
      state.syncStatus == SyncStatus.ERROR -> "Erro"
      state.hasPendingUploads || state.hasPendingDownloads -> "Pendente"
      else -> "Ligado"
    }
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Configuração") },
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
      InfoCard(title = "Dados", titleTextAlign = TextAlign.Center) {
        val serverCount = state.serverClienteCount
        val serverCountLoading =
          configured && serverCount == null && state.syncStatus == SyncStatus.IN_PROGRESS
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          DataTile(
            icon = Icons.Outlined.PhoneAndroid,
            value = state.totalClientes.toString(),
            label = "Na app",
            modifier = Modifier.weight(1f),
          )
          DataTile(
            icon = Icons.Outlined.Cloud,
            value =
              when {
                serverCount != null -> serverCount.toString()
                serverCountLoading -> "A carregar…"
                else -> "—"
              },
            label = "No servidor",
            modifier = Modifier.weight(1f),
            valueColor =
              if (serverCount == null) MaterialTheme.colorScheme.onSurfaceVariant
              else MaterialTheme.colorScheme.onSurface,
          )
        }
        if (serverCount != null) {
          val inSync = serverCount == state.totalClientes
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              if (inSync) Icons.Default.CheckCircle else Icons.Default.CompareArrows,
              contentDescription = null,
              tint =
                if (inSync) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
              if (inSync) "Em sincronia" else "Dados diferentes do servidor",
              style = MaterialTheme.typography.bodySmall,
              color =
                if (inSync) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      InfoCard(title = "Sincronização", titleTextAlign = TextAlign.Center) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          StatusChip(chipLabel, chipContainer, chipContent)
        }
        if (!configured) {
          Text(
            "Importa o ficheiro de credencial do utilizador para começares a sincronizar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
          Button(onClick = importConfig, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Importar credencial do utilizador")
          }
        } else {
          SyncStatusBar(
            syncStatus = state.syncStatus,
            syncFault = state.syncFault,
            hasPendingUploads = state.hasPendingUploads,
            hasPendingDownloads = state.hasPendingDownloads,
            hasConfig = true,
            detail = pendingDetail,
            centered = true,
          )
          state.syncNotice?.let { notice -> NoticeBanner(notice) }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          DetailRow(
            icon = Icons.Outlined.Schedule,
            label = "Última sincronização",
            value = lastSync,
          )
          Button(
            onClick = syncViewModel::manualSync,
            enabled = !syncing,
            modifier = Modifier.fillMaxWidth(),
          ) {
            if (syncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(if (syncing) "A sincronizar…" else "Sincronizar")
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(
          icon = Icons.Outlined.Public,
          label = "URL do servidor",
          value = serverState.baseUrl.ifEmpty { "Não configurado" },
          valueColor =
            if (configured) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(
          icon = Icons.Outlined.Key,
          label = "Chave de assinatura",
          value = if (configured) maskedKey(serverState.signingKey) else "Não configurada",
          valueColor =
            if (configured) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        serverState.signingKeyError?.let { error ->
          Text(
            error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        if (configured) {
          val identity = serverState.keyIdentity
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
          DetailRow(
            icon = Icons.Outlined.Person,
            label = "Utilizador associado",
            value = userLabel,
            valueColor = userColor,
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          OutlinedButton(
            onClick = { showClearConfigDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
          ) {
            Icon(
              Icons.Outlined.DeleteOutline,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Remover credencial")
          }
        }
        ClockWarning(
          skewSeconds = state.clockSkewSeconds,
          correcting = state.clockCorrecting,
          onCorrect = syncViewModel::correctClock,
        )
      }
    }
  }

  if (showClearConfigDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfigDialog = false },
      title = {
        Text(
          "Remover credencial?",
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      text = {
        Text(
          "A credencial (URL do servidor, utilizador e chave de assinatura) será removida " +
            "deste dispositivo. Terás de importar novamente o ficheiro para sincronizar.",
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showClearConfigDialog = false
            serverViewModel.clearConfig()
          }
        ) {
          Text("Remover")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfigDialog = false }) {
          Text("Cancelar")
        }
      },
    )
  }
}

/** One of the comparable counts, on a lighter surface so it reads as its own block. */
@Composable
private fun DataTile(
  icon: ImageVector,
  value: String,
  label: String,
  modifier: Modifier = Modifier,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(12.dp),
    modifier = modifier,
  ) {
    StatTile(
      value = value,
      label = label,
      icon = icon,
      valueColor = valueColor,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
    )
  }
}

/** A server detail: centered icon, small label above the (possibly long) value. */
@Composable
private fun DetailRow(
  icon: ImageVector,
  label: String,
  value: String,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp),
    )
    Spacer(Modifier.height(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = valueColor,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

/** Post-sync feedback, highlighted so it reads as a result rather than as part of the status. */
@Composable
private fun NoticeBanner(text: String) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text,
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    )
  }
}

/** Small state pill on a card header, so the state reads before the details. */
@Composable
private fun StatusChip(label: String, container: Color, content: Color) {
  Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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

/** Banner shown while the device clock is far enough off to threaten sync ordering. */
@Composable
private fun ClockWarning(
  skewSeconds: Long?,
  correcting: Boolean,
  onCorrect: () -> Unit,
) {
  val skew = skewSeconds ?: return
  if (kotlin.math.abs(skew) < CLOCK_SKEW_WARNING_SECONDS) return
  val behind = skew < 0
  val amount = formatSkew(kotlin.math.abs(skew))
  Surface(
    color = MaterialTheme.colorScheme.tertiaryContainer,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(start = 12.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        Icons.Outlined.Schedule,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(10.dp))
      Text(
        text =
          if (behind) {
            "Relógio do dispositivo $amount atrasado. As alterações podem não chegar aos outros dispositivos."
          } else {
            "Relógio do dispositivo $amount adiantado. As alterações podem não chegar aos outros dispositivos."
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onCorrect, enabled = !correcting) {
        Text(if (correcting) "A corrigir…" else "Corrigir")
      }
    }
  }
}

private fun formatSkew(seconds: Long): String =
  when {
    seconds < 3_600 -> "${(seconds / 60).coerceAtLeast(1)} min"
    seconds < 172_800 -> "${seconds / 3_600} h"
    else -> "${seconds / 86_400} dias"
  }

/** Below this skew the warning would be noise (server window is 5 minutes). */
private const val CLOCK_SKEW_WARNING_SECONDS = 120L
