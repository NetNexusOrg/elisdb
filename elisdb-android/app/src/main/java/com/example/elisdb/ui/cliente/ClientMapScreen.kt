package com.example.elisdb.ui.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elisdb.ui.map.MapPin
import com.example.elisdb.ui.map.OsmMapMarkers
import org.osmdroid.util.GeoPoint

private val ActivePinColor = Color(0xFFE53935)
private val InactivePinColor = Color(0xFF9E9E9E)

@Composable
fun ClientMapScreen(
  viewModel: ClientMapViewModel,
  onClienteClick: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  ClientMapScreen(
    state = state,
    onClienteClick = onClienteClick,
    onBack = onBack,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientMapScreen(
  state: ClientMapUiState,
  onClienteClick: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
  val selected = (state as? ClientMapUiState.Success)?.clientes?.firstOrNull { it.id == selectedId }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Mapa de clientes") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
          }
        },
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      when (state) {
        ClientMapUiState.Loading ->
          CircularProgressIndicator(Modifier.align(Alignment.Center))

        is ClientMapUiState.Success ->
          if (state.clientes.isEmpty()) {
            EmptyMapMessage(
              semLocalizacao = state.semLocalizacao,
              modifier = Modifier.align(Alignment.Center),
            )
          } else {
            OsmMapMarkers(
              pins =
                state.clientes.map {
                  MapPin(
                    id = it.id,
                    position = GeoPoint(it.latitude, it.longitude),
                    active = it.ativo,
                  )
                },
              onPinClick = { selectedId = it },
              onMapTapped = { selectedId = null },
              modifier = Modifier.fillMaxSize(),
            )
            MapLegend(
              modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )
            Column(
              modifier =
                Modifier
                  .align(Alignment.BottomCenter)
                  .fillMaxWidth()
                  .navigationBarsPadding()
                  .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (state.semLocalizacao > 0) {
                SemLocalizacaoBanner(state.semLocalizacao)
              }
              selected?.let { cliente ->
                SelectedClientCard(
                  cliente = cliente,
                  onOpen = { onClienteClick(cliente.id) },
                  onClose = { selectedId = null },
                )
              }
            }
          }
      }
    }
  }
}

@Composable
private fun EmptyMapMessage(semLocalizacao: Int, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text =
        if (semLocalizacao > 0) "Nenhum cliente tem localização definida."
        else "Ainda não há clientes.",
      style = MaterialTheme.typography.titleMedium,
    )
    if (semLocalizacao > 0) {
      Text(
        text = semLocalizacaoLabel(semLocalizacao),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SemLocalizacaoBanner(count: Int) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 2.dp,
  ) {
    Text(
      text = semLocalizacaoLabel(count),
      style = MaterialTheme.typography.labelLarge,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
    )
  }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      LegendRow(color = ActivePinColor, label = "Ativo")
      LegendRow(color = InactivePinColor, label = "Inativo")
    }
  }
}

@Composable
private fun LegendRow(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).background(color, CircleShape))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.padding(start = 6.dp),
    )
  }
}

@Composable
private fun SelectedClientCard(
  cliente: ClientMapItem,
  onOpen: () -> Unit,
  onClose: () -> Unit,
) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = cliente.nome,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          cliente.contrato?.takeIf { it.isNotBlank() }?.let { contrato ->
            Text(
              text = contrato,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (!cliente.ativo) {
            Text(
              text = "Inativo",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
        IconButton(onClick = onClose) {
          Icon(Icons.Default.Close, contentDescription = "Fechar")
        }
      }
      Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Text("Abrir cliente")
      }
    }
  }
}

private fun semLocalizacaoLabel(count: Int): String =
  if (count == 1) "1 cliente sem localização" else "$count clientes sem localização"
