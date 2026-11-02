package com.example.elisdb.ui.detalhe

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.location.Localizacao
import com.example.elisdb.ui.map.ElisMap
import com.example.elisdb.ui.map.hasLocationPermission
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

@Composable
fun MapFullscreenScreen(
  repository: ClienteRepository,
  clienteId: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  locationProvider: suspend () -> Localizacao? = { null },
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var myLocation by remember { mutableStateOf<Localizacao?>(null) }
  fun refreshLocation() {
    scope.launch { myLocation = locationProvider() }
  }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
      if (granted.values.any { it }) refreshLocation()
    }

  LaunchedEffect(Unit) {
    if (hasLocationPermission(context)) {
      myLocation = locationProvider()
    } else {
      permissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )
    }
  }

  var cliente by remember { mutableStateOf<ClienteComFotos?>(null) }
  var loading by remember { mutableStateOf(true) }
  var loadError by remember { mutableStateOf(false) }

  LaunchedEffect(clienteId) {
    loading = true
    loadError = false
    cliente =
      try {
        repository.getCliente(clienteId)
      } catch (e: Exception) {
        loadError = true
        null
      }
    loading = false
  }

  Box(modifier = modifier.fillMaxSize()) {
    val coords = cliente?.cliente
    when {
      loading ->
        CircularProgressIndicator(Modifier.align(Alignment.Center))

      loadError ->
        Text(
          "Não foi possível carregar a localização.",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.align(Alignment.Center),
        )

      coords == null ->
        Text(
          "Cliente não encontrado.",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.align(Alignment.Center),
        )

      coords.latitude == null || coords.longitude == null ->
        Text(
          "Sem localização definida.",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.align(Alignment.Center),
        )

      else ->
        ElisMap(
          markerPosition = LatLng(coords.latitude, coords.longitude),
          draggable = false,
          interactive = true,
          modifier = Modifier.fillMaxSize(),
          userLocation = myLocation?.let { LatLng(it.latitude, it.longitude) },
        )
    }

    IconButton(
      onClick = onBack,
      // The map fills the screen behind the system bars, so only the control is
      // inset from the status bar.
      modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
    ) {
      Icon(
        Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Voltar",
        tint = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}