package com.example.elisdb.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.elisdb.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** A client pin shown on the client map. */
data class MapPin(
  val id: String,
  val name: String,
  val position: GeoPoint,
  val active: Boolean = true,
)

/**
 * A read-only map with one pin per item.
 *
 * The map keeps the same [MapView] across recompositions and only adds/removes
 * markers when the pin list changes, so selecting a pin does not reset the zoom.
 */
@Composable
fun OsmMapMarkers(
  pins: List<MapPin>,
  onPinClick: (String) -> Unit,
  modifier: Modifier = Modifier,
  onMapTapped: () -> Unit = {},
) {
  val context = LocalContext.current
  val mapView = rememberMapViewWithLifecycle(context, interactive = true)
  val currentOnPinClick by rememberUpdatedState(onPinClick)
  val currentOnMapTapped by rememberUpdatedState(onMapTapped)
  val markerById = remember { mutableMapOf<String, LabeledMarker>() }
  val lastPinsSignature = remember { arrayOfNulls<String>(1) }

  AndroidView(
    factory = { ctx ->
      markerById.clear()
      mapView.apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        // Pinch is the only zoom gesture: hide osmdroid's +/- buttons.
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        controller.setZoom(15.0)
        controller.setCenter(DEFAULT_CENTER)
        overlays.add(
          MapEventsOverlay(
            object : MapEventsReceiver {
              override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                currentOnMapTapped()
                return true
              }

              override fun longPressHelper(p: GeoPoint): Boolean = true
            }
          )
        )
        addOrUpdateMarkers(this, pins, markerById, currentOnPinClick)
        lastPinsSignature[0] = pinsSignature(pins)
        post { fitPins(this, pins) }
      }
    },
    update = { view ->
      val signature = pinsSignature(pins)
      if (signature != lastPinsSignature[0]) {
        val wantedIds = pins.map { it.id }.toSet()
        markerById.keys
          .filter { it !in wantedIds }
          .forEach { id -> markerById.remove(id)?.let { view.overlays.remove(it) } }
        addOrUpdateMarkers(view, pins, markerById, currentOnPinClick)
        lastPinsSignature[0] = signature
        view.invalidate()
        view.post { fitPins(view, pins) }
      }
    },
    modifier = modifier,
  )
}

private fun addOrUpdateMarkers(
  view: MapView,
  pins: List<MapPin>,
  markerById: MutableMap<String, LabeledMarker>,
  onPinClick: (String) -> Unit,
) {
  val activeIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_map_pin)
  val inactiveIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_map_pin_inactive)
  pins.forEach { pin ->
    val marker =
      markerById[pin.id]
        ?: LabeledMarker(view).also {
          it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
          it.relatedObject = pin.id
          it.setOnMarkerClickListener { clicked, _ ->
            (clicked.relatedObject as? String)?.let(onPinClick)
            true
          }
          view.overlays.add(it)
          markerById[pin.id] = it
        }
    marker.position = pin.position
    marker.icon = if (pin.active) activeIcon else inactiveIcon
    marker.label = pin.name
    marker.setVisible(true)
  }
}

private fun fitPins(view: MapView, pins: List<MapPin>) {
  if (pins.isEmpty()) return
  if (pins.size == 1) {
    view.controller.setZoom(16.0)
    view.controller.setCenter(pins.first().position)
    return
  }
  view.zoomToBoundingBox(
    BoundingBox.fromGeoPoints(pins.map { it.position }),
    false,
    96,
  )
}

private fun pinsSignature(pins: List<MapPin>): String =
  pins.joinToString("|") { pin ->
    "${pin.id}:${pin.name}:${pin.position.latitude}:${pin.position.longitude}:${pin.active}"
  }

/** A map pin that draws the client name on a small label above the icon. */
private class LabeledMarker(mapView: MapView) : Marker(mapView) {
  private val density = mapView.resources.displayMetrics.density
  private val labelPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(33, 33, 33)
      textSize =
        TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_SP,
          12f,
          mapView.resources.displayMetrics,
        )
      typeface = Typeface.DEFAULT_BOLD
      textAlign = Paint.Align.CENTER
    }
  private val backgroundPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(235, 255, 255, 255)
    }
  private val labelPosition = Point()
  private val horizontalPadding = 3f * density
  private val verticalGap = 2f * density
  private val cornerRadius = 4f * density

  var label: String = ""

  override fun draw(canvas: Canvas, projection: Projection) {
    super.draw(canvas, projection)
    if (label.isBlank()) return

    val markerPosition = this.position ?: return
    projection.toPixels(markerPosition, labelPosition)
    val iconHeight = icon?.intrinsicHeight ?: 0
    val baseline = labelPosition.y - iconHeight - verticalGap - labelPaint.descent()
    val textWidth = labelPaint.measureText(label)
    val left = labelPosition.x - textWidth / 2f - horizontalPadding
    val right = labelPosition.x + textWidth / 2f + horizontalPadding
    val top = baseline + labelPaint.ascent() - horizontalPadding
    val bottom = baseline + labelPaint.descent() + horizontalPadding

    canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, backgroundPaint)
    canvas.drawText(label, labelPosition.x.toFloat(), baseline, labelPaint)
  }
}
