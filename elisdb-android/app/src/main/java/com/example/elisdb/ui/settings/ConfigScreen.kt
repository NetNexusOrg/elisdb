package com.example.elisdb.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elisdb.sync.ServerAvailability
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.ui.components.InfoCard
import com.example.elisdb.ui.components.StatTile
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
  var showReplaceLocalDialog by remember { mutableStateOf(false) }
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
  val pendingDetail =
    when {
      state.hasPendingUploads && state.hasPendingDownloads ->
        "${state.pendingUploadCount} por enviar · novidades por receber"
      state.hasPendingUploads -> "${state.pendingUploadCount} por enviar"
      state.hasPendingDownloads -> "Novidades por receber"
      else -> null
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
            value = serverCount?.toString() ?: "—",
            label = "No servidor",
            modifier = Modifier.weight(1f),
            valueColor =
              if (serverCount == null) MaterialTheme.colorScheme.onSurfaceVariant
              else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      InfoCard(title = "Sincronização", titleTextAlign = TextAlign.Center) {
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
          serverState.signingKeyError?.let { ErrorText(it) }
        } else {
          SyncStatusBar(
            syncStatus = state.syncStatus,
            syncFault = state.syncFault,
            hasPendingUploads = state.hasPendingUploads,
            hasPendingDownloads = state.hasPendingDownloads,
            serverAvailability = state.serverAvailability,
            hasSynced = state.lastSuccessAt != null,
            detail = pendingDetail ?: lastSyncDetail(state.lastSuccessAt),
            centered = true,
          )
          if (state.serverAvailability == ServerAvailability.Online) {
            state.syncNotice?.let { notice -> NoticeBanner(notice) }
          }
          if (state.serverIdentityChanged) {
            NoticeBanner(
              "O servidor mudou ou foi restaurado. Os dados locais foram preservados; " +
                "confirma abaixo se queres substituí-los pelos do servidor."
            )
          }
          if (state.localOnlyCount > 0) {
            NoticeBanner(
              "${state.localOnlyCount} registo(s) apenas neste dispositivo: o servidor não os tem. " +
                "Foram preservados e não serão apagados automaticamente."
            )
          }
          if (state.localOnlyCount > 0 || state.serverIdentityChanged) {
            TextButton(
              onClick = { showReplaceLocalDialog = true },
              enabled = !syncing && !state.replacingLocal &&
                state.serverAvailability == ServerAvailability.Online,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                if (state.replacingLocal) "A substituir…"
                else "Substituir dados locais"
              )
            }
          }
          Button(
            onClick = syncViewModel::manualSync,
            enabled = !syncing && state.serverAvailability == ServerAvailability.Online,
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
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "Automático (15 min)",
              style = MaterialTheme.typography.titleSmall,
              modifier = Modifier.weight(1f),
            )
            Switch(
              checked = state.periodicSyncEnabled,
              onCheckedChange = syncViewModel::setPeriodicSync,
            )
          }
          ClockWarning(
            skewSeconds = state.clockSkewSeconds,
            correcting = state.clockCorrecting,
            onCorrect = syncViewModel::correctClock,
          )
          serverState.signingKeyError?.let { ErrorText(it) }
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
          CredentialBlock(
            baseUrl = serverState.baseUrl.ifEmpty { "—" },
            user = userLabel,
            userColor = userColor,
            maskedKey = maskedKey(serverState.signingKey),
          )
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
      }
    }
  }

  if (showReplaceLocalDialog) {
    AlertDialog(
      onDismissRequest = { showReplaceLocalDialog = false },
      title = {
        Text(
          "Substituir dados locais?",
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      text = {
        Text(
          "A app vai criar primeiro uma cópia de segurança local da base de dados e das fotos. " +
            "Depois, os registos que o servidor não tem (incluindo os marcados como apenas locais) " +
            "serão apagados deste dispositivo. Esta ação não pode ser desfeita sem restaurar a cópia.",
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showReplaceLocalDialog = false
            syncViewModel.replaceLocalWithServer()
          }
        ) {
          Text("Substituir")
        }
      },
      dismissButton = {
        TextButton(onClick = { showReplaceLocalDialog = false }) { Text("Cancelar") }
      },
    )
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
          "A credencial (servidor, utilizador e chave) será removida " +
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

/** Compact credential card: URL on its own line, user and key side by side below. */
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

@Composable
private fun CredentialBlock(
  baseUrl: String,
  user: String,
  userColor: Color,
  maskedKey: String,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CredentialField(
        icon = Icons.Outlined.Public,
        label = "Servidor",
        value = baseUrl,
        maxLines = 2,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CredentialField(
          icon = Icons.Outlined.Person,
          label = "Utilizador",
          value = user.uppercase(),
          valueColor = userColor,
          maxLines = 2,
          modifier = Modifier.weight(1f),
        )
        VerticalDivider(
          modifier = Modifier.fillMaxHeight(),
          color = MaterialTheme.colorScheme.outlineVariant,
        )
        CredentialField(
          icon = Icons.Outlined.Key,
          label = "Chave",
          value = maskedKey,
          maxLines = 1,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun CredentialField(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
  maxLines: Int = 1,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(14.dp),
      )
      Spacer(Modifier.width(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
    }
    Spacer(Modifier.height(2.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = valueColor,
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
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

/** Validation error shown next to the credential or sync action it belongs to. */
@Composable
private fun ErrorText(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.error,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth(),
  )
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
        text = if (behind) "Relógio $amount atrasado" else "Relógio $amount adiantado",
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

private fun lastSyncDetail(lastSuccessAt: Long?): String? {
  if (lastSuccessAt == null) return null
  val elapsed =
    (System.currentTimeMillis() / 1000 - lastSuccessAt).coerceAtLeast(0)
  return when {
    elapsed < 60 -> "agora"
    elapsed < 3_600 -> "há ${elapsed / 60} min"
    elapsed < 86_400 -> "há ${elapsed / 3_600} h"
    else -> "há ${elapsed / 86_400} d"
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
