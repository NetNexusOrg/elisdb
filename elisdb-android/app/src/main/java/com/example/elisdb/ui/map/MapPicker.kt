package com.example.elisdb.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPicker(
  initialPosition: GeoPoint?,
  onConfirm: (latitude: Double, longitude: Double) -> Unit,
  onDismiss: () -> Unit,
) {
  var picked by remember { mutableStateOf(initialPosition ?: DEFAULT_CENTER) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Escolher localização") },
          navigationIcon = {
            IconButton(onClick = onDismiss) {
              Icon(Icons.Default.Close, contentDescription = "Cancelar")
            }
          },
        )
      },
    ) { innerPadding ->
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        OsmMap(
          markerPosition = picked,
          draggable = true,
          onMarkerMoved = { point -> picked = point },
          onMapTapped = { point -> picked = point },
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Button(
          onClick = { onConfirm(picked.latitude, picked.longitude) },
          modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
          Icon(Icons.Default.Check, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text("Guardar localização")
        }
      }
    }
  }
}
