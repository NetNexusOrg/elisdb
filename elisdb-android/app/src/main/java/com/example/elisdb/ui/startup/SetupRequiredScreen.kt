package com.example.elisdb.ui.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elisdb.BuildConfig

/**
 * Blocking first screen shown while the app has no complete user configuration.
 * It only offers the way into the (still reachable) configuration screen; the
 * rest of the app is not composed until a credential is imported.
 *
 * @param missingUserName true when a server and key are stored but the user name
 *   is still missing, so the message asks for the credential file again.
 */
@Composable
fun SetupRequiredScreen(
  missingUserName: Boolean,
  onOpenConfig: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(modifier = modifier.fillMaxSize().testTag("setup_required")) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        Icons.Filled.Block,
        contentDescription = null,
        modifier = Modifier.height(72.dp),
        tint = MaterialTheme.colorScheme.error,
      )
      Spacer(Modifier.height(16.dp))
      Text(
        text = "Acesso Negado",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(12.dp))
      Text(
        text =
          if (missingUserName) {
            "Já existe uma chave guardada, mas falta o nome do utilizador. " +
              "Importa novamente as credenciais na Configuração para concluir o acesso."
          } else {
            "A ElisDB é uma app de dados privados.\n" +
              "O acesso é feito com as credenciais de utilizador autorizado."
          },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (!missingUserName) {
        Spacer(Modifier.height(8.dp))
        Text(
          text = "Importa o ficheiro <utilizador>.config.json na Configuração.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
      Spacer(Modifier.height(28.dp))
      Button(onClick = onOpenConfig, modifier = Modifier.testTag("open_config")) {
        Icon(Icons.Default.Settings, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Abrir configuração")
      }
      Spacer(Modifier.height(32.dp))
      Text(
        text = "Versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
