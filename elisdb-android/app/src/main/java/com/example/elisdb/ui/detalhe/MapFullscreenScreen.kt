package com.example.elisdb.ui.detalhe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.ui.map.OsmMap
import org.osmdroid.util.GeoPoint

@Composable
fun MapFullscreenScreen(
  repository: ClienteRepository,
  clienteId: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var cliente by remember { mutableStateOf<ClienteComFotos?>(null) }
  var loading by remember { mutableStateOf(true) }

  LaunchedEffect(clienteId) {
    loading = true
    cliente = repository.getCliente(clienteId)
    loading = false
  }

  Box(modifier = modifier.fillMaxSize()) {
    val coords = cliente?.cliente
    when {
      loading ->
        CircularProgressIndicator(Modifier.align(Alignment.Center))

      coords == null || coords.latitude == null || coords.longitude == null ->
        Text(
          "Sem localização definida.",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.align(Alignment.Center),
        )

      else ->
        OsmMap(
          markerPosition = GeoPoint(coords.latitude, coords.longitude),
          draggable = false,
          interactive = true,
          modifier = Modifier.fillMaxSize(),
        )
    }

    IconButton(
      onClick = onBack,
      modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
    ) {
      Icon(
        Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Voltar",
        tint = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}