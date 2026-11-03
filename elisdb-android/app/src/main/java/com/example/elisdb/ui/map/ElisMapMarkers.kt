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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

internal data class MapPin(
  val id: String,
  val name: String,
  val position: LatLng,
  val active: Boolean = true,
)

@Composable
internal fun ElisMapMarkers(
  pins: List<MapPin>,
  onPinClick: (String) -> Unit,
  modifier: Modifier = Modifier,
  onMapTapped: () -> Unit = {},
  userLocation: LatLng? = null,
  userLocationPending: Boolean = false,
  selectedPinId: String? = null,
) {
  val context = LocalContext.current
  val density = LocalDensity.current.density
  val mapView = rememberMapViewWithLifecycle(context, interactive = true)
  val map =
    rememberMapLibreMap(mapView, interactive = true) { map, latLng ->
      val pinId = map.findClientPinAt(latLng, CLICK_RADIUS_DP * density)
      if (pinId != null) onPinClick(pinId) else onMapTapped()
      true
    }
  val styleUrl = mapStyleUrl(isSystemInDarkTheme())
  val styleState = rememberMapStyle(mapView, styleUrl)
  val laidOut = rememberIsLaidOut(mapView)
  val style = styleState.style
  val signature = pinsSignature(pins)
  var centeredOnUser by remember(mapView) { mutableStateOf(false) }
  var hasFittedPins by remember(mapView) { mutableStateOf(false) }
  var layersReady by remember(style) { mutableStateOf(false) }
  var cameraReady by remember(mapView) { mutableStateOf(false) }
  val mapRendered = rememberMapRenderedState(mapView, styleUrl)

  LaunchedEffect(style) {
    val loadedStyle = style ?: return@LaunchedEffect
    loadedStyle.ensureMapLayers(context = context, pins = pins, user = userLocation)
    layersReady = true
  }

  DisposableEffect(style, map, laidOut, layersReady, signature, selectedPinId, density) {
    val loadedStyle = style
    val loadedMap = map
    if (loadedStyle == null || loadedMap == null || !laidOut || !layersReady) {
      onDispose {}
    } else {
      fun refreshPins() {
        loadedStyle.updateClientPins(
          loadedMap.pinDisplays(pins, selectedPinId, density)
        )
      }
      val listener = MapLibreMap.OnCameraIdleListener { refreshPins() }
      loadedMap.addOnCameraIdleListener(listener)
      refreshPins()
      onDispose { loadedMap.removeOnCameraIdleListener(listener) }
    }
  }

  LaunchedEffect(style, layersReady, userLocation) {
    if (style == null || !layersReady) return@LaunchedEffect
    style.updateUserLocation(userLocation)
  }

  LaunchedEffect(style, map, laidOut, signature, userLocation, userLocationPending) {
    if (style == null || map == null || !laidOut || userLocationPending) return@LaunchedEffect
    val target = mapCameraTarget(
      pins = pins,
      userLocation = userLocation,
      centeredOnUser = centeredOnUser,
      hasFittedPins = hasFittedPins,
    )
    if (target == MapCameraTarget.None) return@LaunchedEffect
    if (!cameraReady) mapRendered.value = false
    map.applyCameraTarget(target)
    cameraReady = true
    if (userLocation != null && !centeredOnUser) {
      centeredOnUser = true
    } else {
      hasFittedPins = true
    }
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
  }
}

private const val CLICK_RADIUS_DP = 24f
