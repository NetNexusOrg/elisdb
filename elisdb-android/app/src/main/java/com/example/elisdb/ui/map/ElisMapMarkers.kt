package com.example.elisdb.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
) {
  val context = LocalContext.current
  val density = LocalDensity.current.density
  val mapView = rememberMapViewWithLifecycle(context, interactive = true)
  val map =
    rememberMapLibreMap(mapView, interactive = true) { map, latLng ->
      val pinId = map.findClientPinAt(latLng, CLICK_RADIUS_DP * density)
      if (pinId != null) {
        onPinClick(pinId)
      } else {
        onMapTapped()
      }
      true
    }
  val styleState = rememberMapStyle(mapView)
  val laidOut = rememberIsLaidOut(mapView)
  val style = styleState.style
  val signature = pinsSignature(pins)
  var centeredOnUser by remember(mapView) { mutableStateOf(false) }
  var hasFittedPins by remember(mapView) { mutableStateOf(false) }
  var layersReady by remember(style) { mutableStateOf(false) }

  LaunchedEffect(style) {
    val loadedStyle = style ?: return@LaunchedEffect
    loadedStyle.ensureMapLayers(context = context, pins = pins, user = userLocation)
    layersReady = true
  }

  LaunchedEffect(style, layersReady, signature) {
    if (style == null || !layersReady) return@LaunchedEffect
    style.updateClientPins(pins)
  }

  LaunchedEffect(style, layersReady, userLocation) {
    if (style == null || !layersReady) return@LaunchedEffect
    style.updateUserLocation(userLocation)
  }

  LaunchedEffect(style, map, laidOut, signature, userLocation) {
    if (style == null || map == null || !laidOut) return@LaunchedEffect
    val target = mapCameraTarget(
      pins = pins,
      userLocation = userLocation,
      centeredOnUser = centeredOnUser,
      hasFittedPins = hasFittedPins,
    )
    if (target == MapCameraTarget.None) return@LaunchedEffect
    map.applyCameraTarget(target)
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
      modifier = Modifier.matchParentSize(),
    )
  }
}

private const val CLICK_RADIUS_DP = 24f
