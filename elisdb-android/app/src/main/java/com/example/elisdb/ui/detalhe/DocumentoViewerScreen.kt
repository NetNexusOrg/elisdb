package com.example.elisdb.ui.detalhe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elisdb.data.local.Foto

/**
 * Full-screen viewer for a device-map image. PDFs are handed to an external
 * reader instead (see the detail screen); a missing file is reported here rather
 * than opening an empty viewer.
 */
@Composable
fun DocumentoViewerScreen(
  viewModel: DocumentoViewerViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
    val documento = state.documento
    when {
      state.isLoading -> Unit
      documento == null || state.fileMissing ->
        Text(
          "O ficheiro já não está disponível no dispositivo.",
          color = Color.White,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.align(Alignment.Center).padding(24.dp),
        )
      else ->
        ZoomablePhoto(
          foto =
            Foto(
              id = documento.id,
              clienteId = documento.clienteId,
              uriLocal = documento.uriLocal,
              mime = documento.mime,
              ordem = documento.ordem,
            ),
          onZoomChanged = {},
          modifier = Modifier.fillMaxSize(),
        )
    }
    IconButton(
      onClick = onBack,
      modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
    ) {
      Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
    }
  }
}
