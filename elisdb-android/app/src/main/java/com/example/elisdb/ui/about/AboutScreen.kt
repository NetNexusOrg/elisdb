package com.example.elisdb.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.elisdb.BuildConfig
import com.example.elisdb.R
import com.example.elisdb.sync.ServerAvailability
import com.example.elisdb.ui.components.InfoCard
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val APP_NAME = "ElisDB"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  onBack: () -> Unit,
  updateViewModel: UpdateViewModel,
  serverAvailability: StateFlow<ServerAvailability> =
    MutableStateFlow(ServerAvailability.Online),
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val phase by updateViewModel.phase.collectAsState()
  val availability by serverAvailability.collectAsState()
  var showQrFullscreen by remember { mutableStateOf(false) }
  LaunchedEffect(updateViewModel, availability) { updateViewModel.check() }
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Sobre") },
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
      AppHeader()
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      InfoCard(title = "Atualizações", titleTextAlign = TextAlign.Center) {
        UpdateSection(
          phase = phase,
          serverAvailable = availability == ServerAvailability.Online,
          onCheck = updateViewModel::check,
          onDownload = updateViewModel::download,
          onRetry = updateViewModel::retry,
          onInstall = { file ->
            if (ApkInstaller.canInstall(context)) {
              ApkInstaller.install(context, file)
            } else {
              ApkInstaller.requestPermission(context)
            }
          },
        )
      }
      InfoCard {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable { showQrFullscreen = true }
              .padding(4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Image(
            painter = painterResource(R.drawable.telegram_qr),
            contentDescription = "QR code do grupo de Telegram",
            modifier =
              Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
          )
          Text(
            "Grupo Telegram",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
          )
          Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }

  if (showQrFullscreen) {
    FullscreenQrDialog(onDismiss = { showQrFullscreen = false })
  }
}

/** Shows the Telegram QR code large enough to be scanned from another device. */
@Composable
private fun FullscreenQrDialog(onDismiss: () -> Unit) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color.Black)
          .clickable(onClick = onDismiss),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(R.drawable.telegram_qr),
        contentDescription = "QR code do grupo de Telegram em ecrã inteiro",
        modifier =
          Modifier
            .fillMaxWidth(0.86f)
            .clip(RoundedCornerShape(24.dp)),
      )
      IconButton(
        onClick = onDismiss,
        modifier =
          Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(16.dp),
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Fechar",
          tint = Color.White,
        )
      }
    }
  }
}

/** App identity: icon, name and the version of the installed build. */
@Composable
private fun AppHeader(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Image(
      painter = painterResource(R.drawable.ic_launcher_foreground),
      contentDescription = APP_NAME,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(88.dp).clip(RoundedCornerShape(22.dp)),
    )
    Spacer(Modifier.height(16.dp))
    Text(
      APP_NAME,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    Text(
      "Versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

/** The body of the "Atualizações" card, one branch per phase of the update flow. */
@Composable
private fun UpdateSection(
  phase: UpdatePhase,
  serverAvailable: Boolean,
  onCheck: () -> Unit,
  onDownload: () -> Unit,
  onRetry: () -> Unit,
  onInstall: (java.io.File) -> Unit,
) {
  var status = "Pronto a verificar"
  var statusColor = MaterialTheme.colorScheme.onSurfaceVariant
  var buttonText = "Procurar atualizações"
  var buttonIcon: ImageVector = Icons.Default.Refresh
  var buttonEnabled = serverAvailable
  var buttonAction: () -> Unit = onCheck
  var busy = false

  when (phase) {
    UpdatePhase.Idle -> Unit
    UpdatePhase.Checking -> {
      status = "A verificar…"
      buttonText = "A verificar…"
      buttonEnabled = false
      busy = true
    }
    UpdatePhase.UpToDate -> status = "Atualizado"
    UpdatePhase.NoRelease -> status = "Sem atualizações"
    UpdatePhase.NotConfigured -> {
      status = "Não configurado"
      buttonEnabled = false
    }
    UpdatePhase.Offline -> {
      status = "Indisponível"
      buttonEnabled = false
    }
    is UpdatePhase.Available -> {
      status = "Versão ${phase.release.versionName} · ${formatSize(phase.release.size)}"
      buttonText = "Transferir"
      buttonIcon = Icons.Default.Download
      buttonAction = onDownload
    }
    is UpdatePhase.Downloading -> {
      val percent = phase.progress?.let { " ${(it * 100).toInt()}%" } ?: ""
      status = "A transferir…$percent"
      buttonEnabled = false
      busy = true
    }
    is UpdatePhase.Ready -> {
      status = "Pronto a instalar"
      buttonText = "Instalar"
      buttonIcon = Icons.Default.SystemUpdate
      buttonAction = { onInstall(phase.file) }
    }
    is UpdatePhase.Failed -> {
      status = when (phase.kind) {
        FailureKind.CHECK -> "Não foi possível verificar"
        FailureKind.DOWNLOAD -> "Não foi possível transferir"
        FailureKind.CHECKSUM -> "Ficheiro inválido"
      }
      statusColor = MaterialTheme.colorScheme.error
      buttonText = "Tentar novamente"
      buttonAction = onRetry
    }
  }

  Text(
    status,
    style = MaterialTheme.typography.bodyMedium,
    color = statusColor,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth(),
  )

  if (phase is UpdatePhase.Downloading) {
    val progress = phase.progress
    if (progress == null) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
      LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
  }

  if (phase !is UpdatePhase.Downloading) {
    Button(
      onClick = buttonAction,
      enabled = buttonEnabled,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (busy) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
          Icon(buttonIcon, contentDescription = null)
        }
        Text(buttonText)
      }
    }
  }
}

/** Human-readable package size, e.g. `17,6 MB`. */
private fun formatSize(bytes: Long): String =
  String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
