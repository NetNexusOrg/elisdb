package com.example.elisdb.ui.detalhe

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.elisdb.data.local.Foto
import java.io.File

internal val PhotoPanOffset = SemanticsPropertyKey<Offset>("PhotoPanOffset")
internal val PhotoPanScale = SemanticsPropertyKey<Float>("PhotoPanScale")
internal var SemanticsPropertyReceiver.photoPanOffset by PhotoPanOffset
internal var SemanticsPropertyReceiver.photoPanScale by PhotoPanScale

@Composable
fun PhotoViewerScreen(
  viewModel: PhotoViewerViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Box(
    modifier = modifier.fillMaxSize().background(Color.Black),
  ) {
    when {
      state.fotos.isEmpty() ->
        Text(
          "Sem fotos para mostrar.",
          color = Color.White,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.align(Alignment.Center),
        )

      else ->
        PhotoPager(
          fotos = state.fotos,
          indice = state.indice,
          onIndiceChange = viewModel::setIndice,
          modifier = Modifier.fillMaxSize(),
        )
    }

    IconButton(
      onClick = onBack,
      // The viewer is edge-to-edge (black background under the system bars), so
      // the controls are inset from the status bar instead of the whole screen.
      modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
    ) {
      Icon(
        Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Voltar",
        tint = Color.White,
      )
    }
  }
}

@Composable
private fun PhotoPager(
  fotos: List<Foto>,
  indice: Int,
  onIndiceChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pagerState =
    rememberPagerState(
      initialPage = indice.coerceIn(0, fotos.size - 1),
    ) { fotos.size }

  var isZoomed by remember { mutableStateOf(false) }

  LaunchedEffect(pagerState.currentPage) {
    onIndiceChange(pagerState.currentPage)
    isZoomed = false
  }

  LaunchedEffect(indice) {
    if (pagerState.currentPage != indice) {
      pagerState.animateScrollToPage(indice)
    }
  }

  Box(modifier) {
    HorizontalPager(
      state = pagerState,
      userScrollEnabled = !isZoomed,
      modifier = Modifier.fillMaxSize(),
    ) { page ->
      val foto = fotos[page]
      key(foto.id) {
        ZoomablePhoto(
          foto = foto,
          onZoomChanged = { isZoomed = it },
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    if (fotos.size > 1) {
      Row(
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
      ) {
        Text(
          text = "${indice + 1} / ${fotos.size}",
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
        )
      }
    }
  }
}

@Composable
internal fun ZoomablePhoto(
  foto: Foto,
  onZoomChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scale = remember { mutableFloatStateOf(1f) }
  val offset = remember { mutableStateOf(Offset.Zero) }
  val containerSize = remember { mutableStateOf(IntSize.Zero) }

  AsyncImage(
    model = File(foto.uriLocal),
    contentDescription = "Foto",
    contentScale = ContentScale.Fit,
    modifier =
      modifier
        .onSizeChanged { containerSize.value = it }
        .graphicsLayer {
          scaleX = scale.floatValue
          scaleY = scale.floatValue
          translationX = offset.value.x
          translationY = offset.value.y
        }
        .semantics {
          photoPanOffset = offset.value
          photoPanScale = scale.floatValue
        }
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
              val event = awaitPointerEvent()
              val pressedCount = event.changes.count { it.pressed }
              if (pressedCount > 1) {
                // Multi-touch: zoom + pan, consumed so the pager does not swipe.
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                val newScale = (scale.floatValue * zoomChange).coerceIn(1f, MAX_SCALE)
                scale.floatValue = newScale
                onZoomChanged(newScale > 1f)
                offset.value =
                  if (newScale <= 1f) {
                    Offset.Zero
                  } else {
                    panOffset(offset.value, panChange, newScale, containerSize.value)
                  }
                event.changes.forEach { it.consume() }
              } else if (scale.floatValue > 1f) {
                // Single finger while zoomed: pan the image (consume) instead of swiping.
                val panChange = event.calculatePan()
                offset.value =
                  panOffset(offset.value, panChange, scale.floatValue, containerSize.value)
                event.changes.forEach { it.consume() }
              }
              // Single finger at 1x: do NOT consume, so the HorizontalPager can swipe.
            } while (event.changes.any { it.pressed })
          }
        },
  )
}

private fun clampOffset(offset: Offset, size: IntSize, scale: Float): Offset {
  if (size == IntSize.Zero) return offset
  val maxX = size.width * (scale - 1f) / 2f
  val maxY = size.height * (scale - 1f) / 2f
  return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

internal fun panOffset(offset: Offset, panChange: Offset, scale: Float, size: IntSize): Offset =
  clampOffset(offset + panChange * scale, size, scale)

private const val MAX_SCALE = 4f
