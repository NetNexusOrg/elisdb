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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.elisdb.BuildConfig
import com.example.elisdb.R
import com.example.elisdb.ui.components.InfoCard
import java.util.Locale

private const val APP_NAME = "ElisDB"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  onBack: () -> Unit,
  updateViewModel: UpdateViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val phase by updateViewModel.phase.collectAsState()
  var showQrFullscreen by remember { mutableStateOf(false) }
  LaunchedEffect(updateViewModel) { updateViewModel.check() }
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
      InfoCard(title = "Atualizações") {
        UpdateSection(
          phase = phase,
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
      InfoCard(title = "Grupo Telegram") {
        Image(
          painter = painterResource(R.drawable.telegram_qr),
          contentDescription = "QR code do grupo de Telegram",
          modifier =
            Modifier
              .align(Alignment.CenterHorizontally)
              .size(180.dp)
              .clip(RoundedCornerShape(16.dp))
              .clickable { showQrFullscreen = true },
        )
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
    )
    Spacer(Modifier.height(4.dp))
    Text(
      "Versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** The body of the "Atualizações" card, one branch per phase of the update flow. */
@Composable
private fun UpdateSection(
  phase: UpdatePhase,
  onCheck: () -> Unit,
  onDownload: () -> Unit,
  onRetry: () -> Unit,
  onInstall: (java.io.File) -> Unit,
) {
  val body = MaterialTheme.typography.bodyMedium
  var message = ""
  var messageColor = MaterialTheme.colorScheme.onSurfaceVariant
  var buttonText = "Procurar atualizações"
  var buttonIcon: ImageVector = Icons.Default.Refresh
  var buttonEnabled = true
  var buttonAction: () -> Unit = onCheck
  var busy = false

  when (phase) {
    UpdatePhase.Idle -> {
      message = "Verifica se há uma versão mais recente publicada no servidor."
    }
    UpdatePhase.Checking -> {
      message = "A contactar o servidor…"
      buttonText = "A procurar atualizações…"
      buttonEnabled = false
      busy = true
    }
    UpdatePhase.UpToDate -> {
      message = "A app está atualizada (versão ${BuildConfig.VERSION_NAME})."
    }
    UpdatePhase.NoRelease -> {
      message = "Não há nenhuma versão publicada no servidor."
    }
    UpdatePhase.NotConfigured -> {
      message = "Configura o servidor para procurar atualizações."
      buttonText = "Servidor não configurado"
      buttonIcon = Icons.Default.Settings
      buttonEnabled = false
    }
    is UpdatePhase.Available -> {
      message = "Toque para transferir · ${formatSize(phase.release.size)}"
      buttonText = "Versão ${phase.release.versionName} disponível"
      buttonIcon = Icons.Default.Download
      buttonAction = onDownload
    }
    is UpdatePhase.Downloading -> {
      message = "A transferir a versão ${phase.release.versionName}…"
      buttonText = "A transferir…"
      buttonIcon = Icons.Default.Download
      buttonEnabled = false
      busy = true
    }
    is UpdatePhase.Ready -> {
      message = "Versão ${phase.release.versionName} transferida e verificada."
      buttonText = "Instalar versão ${phase.release.versionName}"
      buttonIcon = Icons.Default.SystemUpdate
      buttonAction = { onInstall(phase.file) }
    }
    is UpdatePhase.Failed -> {
      message =
        when (phase.kind) {
          FailureKind.CHECK -> "Não foi possível procurar atualizações. Verifica a ligação."
          FailureKind.DOWNLOAD -> "Não foi possível transferir a atualização. Verifica a ligação."
          FailureKind.CHECKSUM ->
            "O ficheiro transferido não corresponde ao publicado. Tenta novamente."
        }
      messageColor = MaterialTheme.colorScheme.error
      buttonText =
        if (phase.release != null) "Tentar transferir novamente" else "Tentar novamente"
      buttonAction = onRetry
    }
  }

  Text(message, style = body, color = messageColor)

  if (phase is UpdatePhase.Downloading) {
    val progress = phase.progress
    if (progress == null) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
      LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
  }

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

/** Human-readable package size, e.g. `17,6 MB`. */
private fun formatSize(bytes: Long): String =
  String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
