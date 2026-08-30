package com.example.elisdb.ui.map

import android.content.Context
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.elisdb.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

internal val DEFAULT_CENTER = GeoPoint(38.7223, -9.1393)

@Composable
fun OsmMap(
  markerPosition: GeoPoint?,
  draggable: Boolean = false,
  interactive: Boolean = true,
  showZoomControls: Boolean = false,
  onMarkerMoved: (GeoPoint) -> Unit = {},
  onMapTapped: (GeoPoint) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val mapView = rememberMapViewWithLifecycle(context, interactive)
  val markerRef = remember { arrayOfNulls<Marker>(1) }
  val currentOnMapTapped by rememberUpdatedState(onMapTapped)
  val currentOnMarkerMoved by rememberUpdatedState(onMarkerMoved)
  var isMapReady by remember { mutableStateOf(false) }

  DisposableEffect(mapView) {
    if (mapView.width > 0 && mapView.height > 0) {
      isMapReady = true
    }
    val listener = ViewTreeObserver.OnGlobalLayoutListener {
      if (mapView.width > 0 && mapView.height > 0) isMapReady = true
    }
    mapView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    onDispose { mapView.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
  }

  Box(modifier = modifier) {
    if (!isMapReady) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxSize(),
      ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
    }

    AndroidView(
      factory = { ctx ->
        mapView.apply {
          setTileSource(TileSourceFactory.MAPNIK)
          setBuiltInZoomControls(showZoomControls)
          setMultiTouchControls(interactive)
          controller.setZoom(16.0)
          overlays.add(
            MapEventsOverlay(
              object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                  currentOnMapTapped(p)
                  return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = true
              }
            )
          )
          val marker =
            Marker(this).apply {
              icon = ctx.getDrawable(R.drawable.ic_map_pin)
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
              isDraggable = draggable
              setOnMarkerDragListener(
                object : Marker.OnMarkerDragListener {
                  override fun onMarkerDragStart(marker: Marker?) = Unit

                  override fun onMarkerDrag(marker: Marker?) {
                    marker?.position?.let { currentOnMarkerMoved(it) }
                  }

                  override fun onMarkerDragEnd(marker: Marker?) {
                    marker?.position?.let { currentOnMarkerMoved(it) }
                  }
                }
              )
            }
          overlays.add(marker)
          markerRef[0] = marker
          if (markerPosition != null) {
            marker.position = markerPosition
            controller.setCenter(markerPosition)
          } else {
            controller.setCenter(DEFAULT_CENTER)
          }
        }
      },
      update = { view ->
        val marker = markerRef[0] ?: return@AndroidView
        val position = markerPosition
        if (position == null) {
          if (draggable) {
            marker.setVisible(true)
            if (marker.position == null) {
              marker.position = DEFAULT_CENTER
            }
          } else {
            marker.setVisible(false)
          }
        } else {
          marker.setVisible(true)
          if (marker.position != position) {
            marker.position = position
            view.controller.animateTo(position)
          }
        }
      },
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Composable
private fun rememberMapViewWithLifecycle(context: Context, interactive: Boolean): MapView {
  val mapView =
    remember {
      val view =
        if (interactive) MapView(context) else NonInteractiveMapView(context)
      view.setTileSource(TileSourceFactory.MAPNIK)
      view
    }
  DisposableEffect(mapView) {
    mapView.onResume()
    onDispose {
      mapView.onPause()
      mapView.onDetach()
    }
  }
  return mapView
}

private class NonInteractiveMapView(context: Context) : MapView(context) {
  override fun onTouchEvent(event: MotionEvent): Boolean = false

  override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
}
