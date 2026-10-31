package com.example.elisdb.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elisdb.BuildConfig
import com.example.elisdb.R
import com.example.elisdb.ui.components.InfoCard
import java.util.Locale

private const val APP_NAME = "ElisDB"
private const val DEVELOPER_NAME = "Ricardo Pinto"
private const val DEVELOPER_EMAIL = "ricardo.pinto@elis.com"
private const val COLLABORATOR_NAME = "João Galado"
private const val COLLABORATOR_EMAIL = "joao.galado@elis.com"
private const val COLLABORATOR_ROLE = "Colaborador · ideias e testes"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  onBack: () -> Unit,
  updateViewModel: UpdateViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val phase by updateViewModel.phase.collectAsState()
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
      InfoCard(title = "Equipa") {
        TeamMember(
          name = DEVELOPER_NAME,
          role = "Desenvolvedor",
          email = DEVELOPER_EMAIL,
          onEmail = { openEmail(context, DEVELOPER_EMAIL) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TeamMember(
          name = COLLABORATOR_NAME,
          role = COLLABORATOR_ROLE,
          email = COLLABORATOR_EMAIL,
          onEmail = { openEmail(context, COLLABORATOR_EMAIL) },
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

/** One person of the team: name, role, and a tappable contact address. */
@Composable
private fun TeamMember(
  name: String,
  role: String,
  email: String,
  onEmail: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      Text(name, style = MaterialTheme.typography.titleMedium)
      Text(
        role,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        email,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEmail),
      )
    }
    IconButton(onClick = onEmail) {
      Icon(Icons.Default.Email, contentDescription = "Enviar email para $name")
    }
  }
}

/** Opens the external email composer; a device without one must not crash. */
private fun openEmail(context: android.content.Context, address: String) {
  val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
  runCatching { context.startActivity(intent) }
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
  when (phase) {
    UpdatePhase.Idle ->
      Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
        Text("Procurar atualizações")
      }
    UpdatePhase.Checking ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text("A procurar atualizações…", style = body)
      }
    UpdatePhase.UpToDate ->
      Text("A app está atualizada (${BuildConfig.VERSION_NAME}).", style = body)
    UpdatePhase.NoRelease -> Text("Não há nenhuma versão publicada no servidor.", style = body)
    UpdatePhase.NotConfigured ->
      Text("Configura o servidor para procurar atualizações.", style = body)
    is UpdatePhase.Available -> {
      Text(
        "Versão ${phase.release.versionName} (${phase.release.versionCode}) · " +
          formatSize(phase.release.size),
        style = body,
      )
      Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("Transferir") }
    }
    is UpdatePhase.Downloading -> {
      Text("A transferir a versão ${phase.release.versionName}…", style = body)
      LinearProgressIndicator(
        progress = { phase.progress ?: 0f },
        modifier = Modifier.fillMaxWidth(),
      )
    }
    is UpdatePhase.Ready -> {
      Text("Versão ${phase.release.versionName} transferida e verificada.", style = body)
      Button(onClick = { onInstall(phase.file) }, modifier = Modifier.fillMaxWidth()) {
        Text("Instalar")
      }
    }
    is UpdatePhase.Failed -> {
      Text(
        when (phase.kind) {
          FailureKind.CHECK -> "Não foi possível procurar atualizações. Verifica a ligação."
          FailureKind.DOWNLOAD -> "Não foi possível transferir a atualização. Verifica a ligação."
          FailureKind.CHECKSUM ->
            "O ficheiro transferido não corresponde ao publicado. Tenta novamente."
        },
        style = body,
        color = MaterialTheme.colorScheme.error,
      )
      OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text("Tentar novamente")
      }
    }
  }
}

/** Human-readable package size, e.g. `17,6 MB`. */
private fun formatSize(bytes: Long): String =
  String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
