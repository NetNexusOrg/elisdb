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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elisdb.location.Localizacao
import com.example.elisdb.ui.map.MapPin
import com.example.elisdb.ui.map.ElisMapMarkers
import com.example.elisdb.ui.map.hasLocationPermission
import org.maplibre.android.geometry.LatLng

@Composable
fun ClientMapScreen(
  viewModel: ClientMapViewModel,
  onClienteClick: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val filtro by viewModel.filtro.collectAsStateWithLifecycle()
  val tagsFiltro by viewModel.tagsFiltro.collectAsStateWithLifecycle()
  val myLocation by viewModel.myLocation.collectAsStateWithLifecycle()
  val locationResolved by viewModel.locationResolved.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
      if (granted.values.any { it }) viewModel.refreshLocation()
      else viewModel.onLocationUnavailable()
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
    tagsFiltro = tagsFiltro,
    myLocation = myLocation,
    userLocationPending = !locationResolved,
    onFiltroChange = viewModel::onFiltroChange,
    onTagToggle = viewModel::onTagToggle,
    onTagsClear = viewModel::onTagsClear,
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
  tagsFiltro: Set<String>,
  onFiltroChange: (ClientMapFilter) -> Unit,
  onTagToggle: (String) -> Unit,
  onTagsClear: () -> Unit,
  onClienteClick: (String) -> Unit,
  onBack: () -> Unit,
  myLocation: Localizacao? = null,
  userLocationPending: Boolean = false,
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
          var page by remember { mutableStateOf(MapFilterMenuPage.ROOT) }
          val closeMenu: () -> Unit = {
            menuOpen = false
            page = MapFilterMenuPage.ROOT
          }
          IconButton(
            onClick = {
              page = MapFilterMenuPage.ROOT
              menuOpen = true
            }
          ) {
            Icon(
              Icons.Default.Tune,
              contentDescription = "Filtrar clientes",
              tint =
                if (filtro == ClientMapFilter.ACTIVE && tagsFiltro.isEmpty()) {
                  MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                  MaterialTheme.colorScheme.primary
                },
            )
          }
          DropdownMenu(expanded = menuOpen, onDismissRequest = closeMenu) {
            when (page) {
              MapFilterMenuPage.ROOT -> {
                val tags = (state as? ClientMapUiState.Success)?.tagsDisponiveis.orEmpty()
                if (tags.isNotEmpty()) {
                  Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                  )
                  DropdownMenuItem(
                    text = { Text("Todas as tags") },
                    onClick = {
                      closeMenu()
                      onTagsClear()
                    },
                    leadingIcon = { FilterCheckIcon(tagsFiltro.isEmpty()) },
                  )
                  tags.forEach { tag ->
                    DropdownMenuItem(
                      text = { Text(tag) },
                      onClick = { onTagToggle(tag) },
                      leadingIcon = { FilterCheckIcon(tag in tagsFiltro) },
                    )
                  }
                  HorizontalDivider()
                }
                DropdownMenuItem(
                  text = { Text("Estado") },
                  onClick = { page = MapFilterMenuPage.STATUS },
                  trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = filtro.shortLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                  },
                )
              }
              MapFilterMenuPage.STATUS -> {
                DropdownMenuItem(
                  text = { Text("Estado") },
                  onClick = { page = MapFilterMenuPage.ROOT },
                  modifier = Modifier.testTag("filter_menu_back"),
                  leadingIcon = {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Voltar")
                  },
                )
                HorizontalDivider()
                ClientMapFilter.entries.forEach { option ->
                  DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                      closeMenu()
                      onFiltroChange(option)
                    },
                    leadingIcon = { FilterCheckIcon(option == filtro) },
                  )
                }
              }
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
          if (state.clientes.isEmpty() && myLocation == null && !userLocationPending) {
            EmptyMapMessage(
              totalClientes = state.totalClientes,
              semLocalizacao = state.semLocalizacao,
              modifier = Modifier.align(Alignment.Center),
            )
          } else {
            ElisMapMarkers(
              pins =
                state.clientes.map {
                  MapPin(
                    id = it.id,
                    name = it.nome,
                    position = LatLng(it.latitude, it.longitude),
                    active = it.ativo,
                  )
                },
              onPinClick = { selectedId = it },
              onMapTapped = { selectedId = null },
              userLocation = myLocation?.let { LatLng(it.latitude, it.longitude) },
              userLocationPending = userLocationPending,
              selectedPinId = selectedId,
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

private enum class MapFilterMenuPage { ROOT, STATUS }

private fun ClientMapFilter.shortLabel(): String =
  when (this) {
    ClientMapFilter.ALL -> "Todos"
    ClientMapFilter.ACTIVE -> "Ativos"
    ClientMapFilter.INACTIVE -> "Inativos"
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
