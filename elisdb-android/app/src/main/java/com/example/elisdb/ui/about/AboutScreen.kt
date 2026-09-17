package com.example.elisdb.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.elisdb.BuildConfig
import com.example.elisdb.ui.components.InfoCard
import com.example.elisdb.ui.components.InfoRow

private const val APP_NAME = "ElisDB"
private const val APP_DESCRIPTION =
  "Gestão de clientes offline-first. Os dados ficam guardados no dispositivo e " +
    "sincronizam com o servidor quando há ligação."
private const val DEVELOPER_NAME = "RP"
private const val DEVELOPER_EMAIL = "kadu247@protonmail.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
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
      InfoCard(title = "A aplicação") {
        InfoRow(label = "Nome", value = APP_NAME)
        InfoRow(
          label = "Versão",
          value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        )
        InfoRow(label = "Descrição", value = APP_DESCRIPTION)
      }
      InfoCard(title = "Desenvolvedor") {
        InfoRow(label = "Nome", value = DEVELOPER_NAME)
        InfoRow(
          label = "Contacto",
          value = DEVELOPER_EMAIL,
          onValueClick = {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$DEVELOPER_EMAIL"))
            runCatching { context.startActivity(intent) }
          },
        )
      }
    }
  }
}
