package com.example.elisdb.ui.cliente

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elisdb.location.Localizacao
import com.example.elisdb.ui.map.MapPin
import com.example.elisdb.ui.map.OsmMapMarkers
import com.example.elisdb.ui.map.hasLocationPermission
import org.osmdroid.util.GeoPoint

@Composable
fun ClientMapScreen(
  viewModel: ClientMapViewModel,
  onClienteClick: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val filtro by viewModel.filtro.collectAsStateWithLifecycle()
  val myLocation by viewModel.myLocation.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
      if (granted.values.any { it }) viewModel.refreshLocation()
    }

  LaunchedEffect(Unit) {
    if (hasLocationPermission(context)) {
      viewModel.refreshLocation()
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )
    }
  }

  ClientMapScreen(
    state = state,
    filtro = filtro,
    myLocation = myLocation,
    onFiltroChange = viewModel::onFiltroChange,
    onClienteClick = onClienteClick,
    onBack = onBack,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientMapScreen(
  state: ClientMapUiState,
  filtro: ClientMapFilter,
  onFiltroChange: (ClientMapFilter) -> Unit,
  onClienteClick: (String) -> Unit,
  onBack: () -> Unit,
  myLocation: Localizacao? = null,
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
        actions = {
          var menuOpen by remember { mutableStateOf(false) }
          IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.Menu, contentDescription = "Filtrar clientes")
          }
          DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            ClientMapFilter.entries.forEach { option ->
              DropdownMenuItem(
                text = { Text(option.label) },
                onClick = {
                  menuOpen = false
                  onFiltroChange(option)
                },
                leadingIcon = {
                  if (option == filtro) {
                    Icon(Icons.Default.Check, contentDescription = null)
                  } else {
                    Spacer(Modifier.size(24.dp))
                  }
                },
              )
            }
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
          if (state.clientes.isEmpty() && myLocation == null) {
            EmptyMapMessage(
              totalClientes = state.totalClientes,
              semLocalizacao = state.semLocalizacao,
              modifier = Modifier.align(Alignment.Center),
            )
          } else {
            OsmMapMarkers(
              pins =
                state.clientes.map {
                  MapPin(
                    id = it.id,
                    name = it.nome,
                    position = GeoPoint(it.latitude, it.longitude),
                    active = it.ativo,
                  )
                },
              onPinClick = { selectedId = it },
              onMapTapped = { selectedId = null },
              userLocation = myLocation?.let { GeoPoint(it.latitude, it.longitude) },
              modifier = Modifier.fillMaxSize(),
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
              if (state.clientes.isEmpty()) {
                Text(
                  text = emptyMapMessage(state.totalClientes, state.semLocalizacao),
                  modifier = Modifier.fillMaxWidth(),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
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
private fun EmptyMapMessage(
  totalClientes: Int,
  semLocalizacao: Int,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = emptyMapMessage(totalClientes, semLocalizacao),
      style = MaterialTheme.typography.titleMedium,
    )
  }
}

private fun emptyMapMessage(totalClientes: Int, semLocalizacao: Int): String =
  when {
    totalClientes == 0 -> "Ainda não há clientes."
    semLocalizacao == totalClientes -> "Nenhum cliente tem localização definida."
    else -> "Nenhum cliente corresponde ao filtro."
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
