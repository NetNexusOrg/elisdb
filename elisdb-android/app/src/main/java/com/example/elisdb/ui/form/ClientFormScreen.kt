package com.example.elisdb.ui.form

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.elisdb.data.local.Foto
import com.example.elisdb.ui.map.MapPicker
import com.example.elisdb.ui.map.ElisMap
import java.io.File
import org.maplibre.android.geometry.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormScreen(
  viewModel: ClientFormViewModel,
  clienteId: String?,
  onSaved: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.saved.collect { id -> onSaved(id) }
  }

  LaunchedEffect(state.message) {
    state.message?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.onMessageShown()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text(if (state.isNew) "Novo cliente" else "Editar cliente") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.Close, contentDescription = "Fechar")
          }
        },
        actions = {
          TextButton(onClick = viewModel::save, enabled = !state.saving) { Text("Guardar") }
        },
      )
    },
  ) { innerPadding ->
    Box(Modifier.padding(innerPadding).fillMaxSize()) {
      if (state.showCamera) {
        CameraCapture(
          createOutputFile = viewModel::createPhotoFile,
          onPhotoCaptured = viewModel::onPhotoCaptured,
          onCancel = viewModel::dismissCamera,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        ClientFormContent(
          state = state,
          onNomeChange = viewModel::onNomeChange,
          onContratoChange = viewModel::onContratoChange,
          onTagChange = viewModel::onTagChange,
          onNotasChange = viewModel::onNotasChange,
          onAtivoChange = viewModel::onAtivoChange,
          onLocationChange = viewModel::onLocationChange,
          onUseCurrentLocation = viewModel::useCurrentLocation,
          onPickPhoto = viewModel::onPhotoPicked,
          onTakePhoto = viewModel::showCamera,
          onRemoveFoto = viewModel::onRemoveFoto,
        )
      }
      if (state.saving) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}

@Composable
private fun ClientFormContent(
  state: ClientFormUiState,
  onNomeChange: (String) -> Unit,
  onContratoChange: (String) -> Unit,
  onTagChange: (String) -> Unit,
  onNotasChange: (String) -> Unit,
  onAtivoChange: (Boolean) -> Unit,
  onLocationChange: (Double, Double) -> Unit,
  onUseCurrentLocation: () -> Unit,
  onPickPhoto: (Uri) -> Unit,
  onTakePhoto: () -> Unit,
  onRemoveFoto: (Foto) -> Unit,
) {
  val context = LocalContext.current
  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      uri?.let(onPickPhoto)
    }
  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        onTakePhoto()
      } else {
        Toast.makeText(context, "Sem permissão para usar a câmara.", Toast.LENGTH_SHORT).show()
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        "Cliente ativo",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f),
      )
      Switch(checked = state.ativo, onCheckedChange = onAtivoChange)
    }

    OutlinedTextField(
      value = state.contrato,
      onValueChange = onContratoChange,
      label = { Text("Contrato") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
      value = state.tag,
      onValueChange = onTagChange,
      label = { Text("Tag") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
      value = state.nome,
      onValueChange = onNomeChange,
      label = { Text("Nome *") },
      isError = state.nomeError,
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
      value = state.notas,
      onValueChange = onNotasChange,
      label = { Text("Notas") },
      minLines = 2,
      modifier = Modifier.fillMaxWidth(),
    )

    Text("Localização", style = MaterialTheme.typography.titleMedium)
    var showMapPicker by remember { mutableStateOf(false) }
    val position =
      if (state.latitude != null && state.longitude != null) {
        LatLng(state.latitude, state.longitude)
      } else {
        null
      }
    Box(
      modifier =
        Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
    ) {
      ElisMap(
        markerPosition = position,
        draggable = false,
        interactive = false,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier
            .matchParentSize()
            .clickable { showMapPicker = true }
            .testTag("map_preview"),
      )
    }
    if (showMapPicker) {
      MapPicker(
        initialPosition = position,
        onConfirm = { latitude, longitude ->
          onLocationChange(latitude, longitude)
          showMapPicker = false
        },
        onDismiss = { showMapPicker = false },
      )
    }
    LocationPermissionButton(onUseCurrentLocation = onUseCurrentLocation)

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Button(
        onClick = {
          val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
              PackageManager.PERMISSION_GRANTED
          if (granted) {
            onTakePhoto()
          } else {
            cameraLauncher.launch(Manifest.permission.CAMERA)
          }
        },
        modifier = Modifier.weight(1f),
      ) {
        Icon(Icons.Default.PhotoCamera, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Foto")
      }
      Button(
        onClick = {
          photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        modifier = Modifier.weight(1f),
      ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Galeria")
      }
    }

    if (state.fotos.isNotEmpty()) {
      PhotoGrid(fotos = state.fotos, onRemove = onRemoveFoto)
    }

    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun LocationPermissionButton(onUseCurrentLocation: () -> Unit) {
  val context = LocalContext.current
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
        it[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      ) {
        onUseCurrentLocation()
      } else {
        Toast.makeText(context, "Sem permissão de localização.", Toast.LENGTH_SHORT).show()
      }
    }

  Button(
    onClick = {
      val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
      val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
      if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
        onUseCurrentLocation()
      } else {
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
      }
    },
    modifier = Modifier.fillMaxWidth(),
  ) {
    Icon(Icons.Default.GpsFixed, contentDescription = null)
    Spacer(Modifier.size(8.dp))
    Text("Usar localização atual")
  }
}

@Composable
private fun PhotoGrid(fotos: List<Foto>, onRemove: (Foto) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    fotos.chunked(3).forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        row.forEach { foto ->
          Box(modifier = Modifier.weight(1f)) {
            AsyncImage(
              model = File(foto.uriLocal),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(100.dp)
                  .clip(RoundedCornerShape(8.dp)),
            )
            IconButton(
              onClick = { onRemove(foto) },
              modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
            ) {
              Icon(Icons.Default.Close, contentDescription = "Remover foto", modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }
}
