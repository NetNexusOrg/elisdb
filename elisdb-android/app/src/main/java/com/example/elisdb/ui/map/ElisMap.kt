package com.example.elisdb.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

@Composable
internal fun ElisMap(
  markerPosition: LatLng?,
  modifier: Modifier = Modifier,
  draggable: Boolean = false,
  interactive: Boolean = true,
  onMarkerMoved: (LatLng) -> Unit = {},
  onMapTapped: (LatLng) -> Unit = {},
  userLocation: LatLng? = null,
) {
  val context = LocalContext.current
  val mapView = rememberMapViewWithLifecycle(context, interactive)
  val map =
    rememberMapLibreMap(mapView, interactive) { _, latLng ->
      onMapTapped(latLng)
      true
    }
  val styleUrl = mapStyleUrl(isSystemInDarkTheme())
  val styleState = rememberMapStyle(mapView, styleUrl)
  val laidOut = rememberIsLaidOut(mapView)
  val style = styleState.style

  val latestStyle by rememberUpdatedState(style)
  val latestMap by rememberUpdatedState(map)
  val latestMarker by rememberUpdatedState(markerPosition)
  val latestOnMarkerMoved by rememberUpdatedState(onMarkerMoved)
  var centered by remember(mapView) { mutableStateOf(false) }
  var lastCenteredMarker by remember(mapView) { mutableStateOf<LatLng?>(null) }
  var layersReady by remember(style) { mutableStateOf(false) }
  var cameraReady by remember(mapView) { mutableStateOf(false) }
  val mapRendered = rememberMapRenderedState(mapView, styleUrl)

  LaunchedEffect(style) {
    val loadedStyle = style ?: return@LaunchedEffect
    loadedStyle.ensureMapLayers(
      context = context,
      picked = markerPosition ?: if (draggable) DEFAULT_CENTER else null,
      user = userLocation,
    )
    layersReady = true
  }

  LaunchedEffect(style, layersReady, markerPosition, draggable) {
    if (style == null || !layersReady) return@LaunchedEffect
    style.updatePickedLocation(
      markerPosition ?: if (draggable) DEFAULT_CENTER else null,
    )
  }

  LaunchedEffect(style, layersReady, userLocation) {
    if (style == null || !layersReady) return@LaunchedEffect
    style.updateUserLocation(userLocation)
  }

  LaunchedEffect(style, map, laidOut, markerPosition, draggable) {
    if (style == null || map == null || !laidOut) return@LaunchedEffect
    val shouldCenter = !centered || (!draggable && markerPosition != lastCenteredMarker)
    if (!shouldCenter) return@LaunchedEffect
    val target = markerPosition ?: DEFAULT_CENTER
    if (!cameraReady) mapRendered.value = false
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, MAP_DEFAULT_ZOOM))
    centered = true
    lastCenteredMarker = markerPosition
    cameraReady = true
  }

  val controller =
    remember(mapView) {
      DraggableMarkerController(
        mapView = mapView,
        mapProvider = { latestMap },
        positionProvider = { latestMarker ?: DEFAULT_CENTER },
        onMoved = { position ->
          latestStyle?.updatePickedLocation(position)
          latestOnMarkerMoved(position)
        },
        onDragStarted = {},
        onDragEnded = {},
      )
    }

  DisposableEffect(draggable, controller) {
    if (draggable) controller.attach() else controller.detach()
    onDispose { controller.detach() }
  }

  Box(modifier) {
    AndroidView(
      factory = { mapView },
      modifier = Modifier.fillMaxSize(),
    )
    MapStatusOverlay(
      state = styleState,
      laidOut = laidOut,
      cameraReady = cameraReady,
      mapRendered = mapRendered.value,
      modifier = Modifier.matchParentSize(),
    )
    MapAttribution(modifier = Modifier.matchParentSize())
  }
}
