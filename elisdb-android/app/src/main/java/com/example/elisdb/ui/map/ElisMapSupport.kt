package com.example.elisdb.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.elisdb.R
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

internal const val MAP_STYLE_LIGHT_URL = "https://tiles.openfreemap.org/styles/liberty"
internal const val MAP_STYLE_DARK_URL = "https://tiles.openfreemap.org/styles/liberty"
internal const val MAP_DEFAULT_ZOOM = 16.0
internal const val MAP_USER_ZOOM = 15.0
internal const val MAP_PINS_SOURCE_ID = "client-pins"
internal const val MAP_PINS_LAYER_ID = "client-pins-layer"
internal const val MAP_PIN_DOT_LAYER_ID = "client-pin-dots"
internal const val MAP_PICKED_SOURCE_ID = "picked-location"
internal const val MAP_PICKED_LAYER_ID = "picked-location-layer"
internal const val MAP_USER_SOURCE_ID = "user-location"
internal const val MAP_USER_LAYER_ID = "user-location-layer"
internal const val MAP_PIN_ACTIVE_IMAGE = "pin-active"
internal const val MAP_PIN_INACTIVE_IMAGE = "pin-inactive"
internal const val MAP_PIN_DOT_ACTIVE_IMAGE = "pin-dot-active"
internal const val MAP_PIN_DOT_INACTIVE_IMAGE = "pin-dot-inactive"
internal const val MAP_USER_LOCATION_IMAGE = "user-location"

internal const val PIN_COLLISION_RADIUS_DP = 34f
internal const val PIN_MIN_SCALE = 0.36f
internal const val PIN_DOT_SCALE = 0.55f
internal const val PIN_LABEL_SCALE = 0.85f
internal const val PIN_FAN_MIN_ZOOM = 14.0
internal const val PIN_FAN_BASE_DP = 10f
internal const val PIN_FAN_STEP_DP = 2f

internal val DEFAULT_CENTER = LatLng(38.7223, -9.1393)

internal fun mapStyleUrl(isDark: Boolean): String =
  if (isDark) MAP_STYLE_DARK_URL else MAP_STYLE_LIGHT_URL

internal class MapStyleState(
  val style: Style?,
  val failed: Boolean,
  val retry: () -> Unit,
)

@Composable
internal fun rememberMapViewWithLifecycle(context: Context, interactive: Boolean): MapView {
  val lifecycleOwner = LocalLifecycleOwner.current
  val mapView =
    remember(context, interactive, lifecycleOwner) {
      val options = MapLibreMapOptions.createFromAttributes(context).textureMode(true)
      val view =
        if (interactive) MapView(context, options) else NonInteractiveMapView(context, options)
      view.onCreate(null)
      view
    }

  DisposableEffect(lifecycleOwner, mapView) {
    var attached = mapView.isAttachedToWindow
    var started = false
    var resumed = false
    var destroyed = false

    fun start() {
      if (attached && !started && !destroyed) {
        mapView.onStart()
        started = true
      }
    }

    fun resume() {
      if (!attached || destroyed) return
      start()
      if (!resumed) {
        mapView.onResume()
        resumed = true
      }
    }

    fun pause() {
      if (resumed && !destroyed) {
        mapView.onPause()
        resumed = false
      }
    }

    fun stop() {
      pause()
      if (started && !destroyed) {
        mapView.onStop()
        started = false
      }
    }

    fun destroy() {
      stop()
      if (!destroyed) {
        mapView.onDestroy()
        destroyed = true
      }
    }

    fun sync() {
      if (destroyed) return
      if (!attached) {
        stop()
        return
      }
      when {
        lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED -> resume()
        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> start()
        else -> stop()
      }
    }

    val observer =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_START -> if (attached) start()
          Lifecycle.Event.ON_RESUME -> if (attached) resume()
          Lifecycle.Event.ON_PAUSE -> pause()
          Lifecycle.Event.ON_STOP -> stop()
          Lifecycle.Event.ON_DESTROY -> destroy()
          else -> Unit
        }
      }

    val attachListener =
      object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
          attached = true
          sync()
        }

        override fun onViewDetachedFromWindow(view: View) {
          attached = false
          stop()
        }
      }

    mapView.addOnAttachStateChangeListener(attachListener)
    lifecycleOwner.lifecycle.addObserver(observer)
    sync()

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      mapView.removeOnAttachStateChangeListener(attachListener)
      destroy()
    }
  }
  return mapView
}

@Composable
internal fun rememberMapLibreMap(
  mapView: MapView,
  interactive: Boolean,
  onMapClick: (MapLibreMap, LatLng) -> Boolean,
): MapLibreMap? {
  var map by remember(mapView) { mutableStateOf<MapLibreMap?>(null) }
  val currentOnMapClick by rememberUpdatedState(onMapClick)

  val listener =
    remember(mapView) {
      MapLibreMap.OnMapClickListener { latLng ->
        map?.let { currentOnMapClick(it, latLng) } ?: false
      }
    }

  DisposableEffect(mapView, interactive) {
    var disposed = false
    mapView.getMapAsync { instance ->
      if (disposed) return@getMapAsync
      map = instance
      instance.configureUi(interactive)
      instance.removeOnMapClickListener(listener)
      instance.addOnMapClickListener(listener)
    }
    onDispose {
      disposed = true
      map?.removeOnMapClickListener(listener)
    }
  }
  return map
}

@Composable
internal fun rememberMapStyle(mapView: MapView, styleUrl: String): MapStyleState {
  var style by remember(mapView, styleUrl) { mutableStateOf<Style?>(null) }
  var failed by remember(mapView, styleUrl) { mutableStateOf(false) }
  var attempt by remember(mapView, styleUrl) { mutableIntStateOf(0) }
  val retry =
    remember(mapView, styleUrl) {
      {
        style = null
        failed = false
        attempt += 1
      }
    }

  DisposableEffect(mapView, styleUrl, attempt) {
    var disposed = false
    val failureListener =
      MapView.OnDidFailLoadingMapListener {
        if (!disposed) failed = true
      }
    mapView.addOnDidFailLoadingMapListener(failureListener)
    mapView.getMapAsync { map ->
      if (disposed) return@getMapAsync
      val camera = if (map.style != null) map.cameraPosition else null
      map.setStyle(styleUrl) { loadedStyle ->
        if (!disposed) {
          camera?.let { map.moveCamera(CameraUpdateFactory.newCameraPosition(it)) }
          style = loadedStyle
          failed = false
        }
      }
    }
    onDispose {
      disposed = true
      mapView.removeOnDidFailLoadingMapListener(failureListener)
    }
  }
  return MapStyleState(style, failed, retry)
}

@Composable
internal fun rememberIsLaidOut(view: View): Boolean {
  var laidOut by remember(view) { mutableStateOf(view.width > 0 && view.height > 0) }
  DisposableEffect(view) {
    val listener =
      ViewTreeObserver.OnGlobalLayoutListener {
        if (view.width > 0 && view.height > 0) laidOut = true
      }
    view.viewTreeObserver.addOnGlobalLayoutListener(listener)
    onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
  }
  return laidOut
}

@Composable
internal fun rememberMapRenderedState(mapView: MapView, styleUrl: String): MutableState<Boolean> {
  val rendered = remember(mapView, styleUrl) { mutableStateOf(false) }
  DisposableEffect(mapView, styleUrl) {
    val listener =
      MapView.OnDidFinishRenderingFrameListener { fullyRendered, _, _ ->
        if (fullyRendered) rendered.value = true
      }
    mapView.addOnDidFinishRenderingFrameListener(listener)
    onDispose { mapView.removeOnDidFinishRenderingFrameListener(listener) }
  }
  return rendered
}

@Composable
internal fun MapStatusOverlay(
  state: MapStyleState,
  laidOut: Boolean,
  cameraReady: Boolean = true,
  mapRendered: Boolean = true,
  modifier: Modifier = Modifier,
) {
  when {
    state.failed ->
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxSize(),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text("Não foi possível carregar o mapa.")
          TextButton(onClick = state.retry) { Text("Tentar novamente") }
        }
      }

    !laidOut || state.style == null || !cameraReady || !mapRendered ->
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxSize(),
      ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
  }
}

internal fun MapLibreMap.configureUi(interactive: Boolean) {
  uiSettings.apply {
    setAllGesturesEnabled(interactive)
    setRotateGesturesEnabled(false)
    setTiltGesturesEnabled(false)
    setCompassEnabled(false)
    setLogoEnabled(false)
    setAttributionEnabled(false)
  }
}

internal fun Style.ensureMapLayers(
  context: Context,
  pins: List<MapPin> = emptyList(),
  picked: LatLng? = null,
  user: LatLng? = null,
) {
  addMapImages(context)
  addClientPinsLayer(pins)
  addPickedLocationLayer(picked)
  addUserLocationLayer(user)
}

internal fun Style.addMapImages(context: Context) {
  if (getImage(MAP_PIN_ACTIVE_IMAGE) == null) {
    addImage(
      MAP_PIN_ACTIVE_IMAGE,
      requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_pin)),
    )
  }
  if (getImage(MAP_PIN_INACTIVE_IMAGE) == null) {
    addImage(
      MAP_PIN_INACTIVE_IMAGE,
      requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_pin_inactive)),
    )
  }
  if (getImage(MAP_PIN_DOT_ACTIVE_IMAGE) == null) {
    addImage(
      MAP_PIN_DOT_ACTIVE_IMAGE,
      requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_dot_active)),
    )
  }
  if (getImage(MAP_PIN_DOT_INACTIVE_IMAGE) == null) {
    addImage(
      MAP_PIN_DOT_INACTIVE_IMAGE,
      requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_dot_inactive)),
    )
  }
  if (getImage(MAP_USER_LOCATION_IMAGE) == null) {
    addImage(
      MAP_USER_LOCATION_IMAGE,
      requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_user_location)),
    )
  }
}

internal fun Style.addClientPinsLayer(pins: List<MapPin> = emptyList()) {
  if (getSourceAs<GeoJsonSource>(MAP_PINS_SOURCE_ID) == null) {
    addSource(
      GeoJsonSource(
        MAP_PINS_SOURCE_ID,
        FeatureCollection.fromFeatures(pins.map(::clientPinFeature)),
      )
    )
  }
  if (getLayerAs<SymbolLayer>(MAP_PIN_DOT_LAYER_ID) == null) {
    addLayer(
      SymbolLayer(MAP_PIN_DOT_LAYER_ID, MAP_PINS_SOURCE_ID)
        .withProperties(*dotLayerProperties())
        .withFilter(Expression.eq(Expression.get("kind"), Expression.literal("dot"))),
    )
  }
  if (getLayerAs<SymbolLayer>(MAP_PINS_LAYER_ID) == null) {
    addLayer(
      SymbolLayer(MAP_PINS_LAYER_ID, MAP_PINS_SOURCE_ID)
        .withProperties(*pinLayerProperties())
        .withFilter(Expression.eq(Expression.get("kind"), Expression.literal("pin"))),
    )
  }
}

internal fun Style.addPickedLocationLayer(position: LatLng? = null) {
  if (getSourceAs<GeoJsonSource>(MAP_PICKED_SOURCE_ID) == null) {
    addSource(
      GeoJsonSource(
        MAP_PICKED_SOURCE_ID,
        FeatureCollection.fromFeatures(
          if (position == null) {
            emptyList()
          } else {
            listOf(pointFeature(position, MAP_PIN_ACTIVE_IMAGE))
          },
        ),
      )
    )
  }
  if (getLayerAs<SymbolLayer>(MAP_PICKED_LAYER_ID) == null) {
    addLayer(
      SymbolLayer(MAP_PICKED_LAYER_ID, MAP_PICKED_SOURCE_ID)
        .withProperties(
          PropertyFactory.iconImage(MAP_PIN_ACTIVE_IMAGE),
          PropertyFactory.iconSize(1f),
          PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
          PropertyFactory.iconAllowOverlap(true),
          PropertyFactory.iconIgnorePlacement(true),
        )
    )
  }
}

internal fun Style.addUserLocationLayer(position: LatLng? = null) {
  if (getSourceAs<GeoJsonSource>(MAP_USER_SOURCE_ID) == null) {
    addSource(
      GeoJsonSource(
        MAP_USER_SOURCE_ID,
        FeatureCollection.fromFeatures(
          if (position == null) {
            emptyList()
          } else {
            listOf(pointFeature(position, MAP_USER_LOCATION_IMAGE))
          },
        ),
      )
    )
  }
  if (getLayerAs<SymbolLayer>(MAP_USER_LAYER_ID) == null) {
    addLayer(
      SymbolLayer(MAP_USER_LAYER_ID, MAP_USER_SOURCE_ID)
        .withProperties(
          PropertyFactory.iconImage(MAP_USER_LOCATION_IMAGE),
          PropertyFactory.iconSize(1f),
          PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
          PropertyFactory.iconAllowOverlap(true),
          PropertyFactory.iconIgnorePlacement(true),
        )
    )
  }
}

private fun pinLayerProperties(): Array<PropertyValue<*>> =
  arrayOf(
    PropertyFactory.iconImage(Expression.get("icon")),
    PropertyFactory.iconSize(Expression.get("size")),
    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
    PropertyFactory.iconAllowOverlap(true),
    PropertyFactory.iconIgnorePlacement(true),
    PropertyFactory.textField(Expression.get("label")),
    PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
    PropertyFactory.textSize(12f),
    PropertyFactory.textColor("#212121"),
    PropertyFactory.textHaloColor("#FFFFFF"),
    PropertyFactory.textHaloWidth(2f),
    PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
    PropertyFactory.textOffset(arrayOf(0f, -2.4f)),
    PropertyFactory.textMaxWidth(8f),
    PropertyFactory.textOptional(true),
    PropertyFactory.textAllowOverlap(false),
    PropertyFactory.textIgnorePlacement(false),
  )

private fun dotLayerProperties(): Array<PropertyValue<*>> =
  arrayOf(
    PropertyFactory.iconImage(Expression.get("icon")),
    PropertyFactory.iconSize(Expression.get("size")),
    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
    PropertyFactory.iconAllowOverlap(true),
    PropertyFactory.iconIgnorePlacement(true),
  )

internal fun Style.updatePickedLocation(position: LatLng?) {
  getSourceAs<GeoJsonSource>(MAP_PICKED_SOURCE_ID)?.setGeoJson(
    if (position == null) {
      emptyFeatureCollection()
    } else {
      FeatureCollection.fromFeatures(
        listOf(pointFeature(position, MAP_PIN_ACTIVE_IMAGE)),
      )
    }
  )
}

internal fun Style.updateUserLocation(position: LatLng?) {
  getSourceAs<GeoJsonSource>(MAP_USER_SOURCE_ID)?.setGeoJson(
    if (position == null) {
      emptyFeatureCollection()
    } else {
      FeatureCollection.fromFeatures(
        listOf(pointFeature(position, MAP_USER_LOCATION_IMAGE)),
      )
    }
  )
}

internal data class ScreenPoint(val x: Float, val y: Float)

internal data class PinDisplay(
  val pin: MapPin,
  val position: LatLng,
  val icon: String,
  val size: Float,
  val isDot: Boolean,
  val label: String?,
)

internal fun Style.updateClientPins(displays: List<PinDisplay>) {
  getSourceAs<GeoJsonSource>(MAP_PINS_SOURCE_ID)?.setGeoJson(
    FeatureCollection.fromFeatures(displays.map(::clientPinFeature)),
  )
}

internal fun clientPinFeature(display: PinDisplay): Feature =
  Feature.fromGeometry(
    Point.fromLngLat(display.position.longitude, display.position.latitude)
  ).apply {
    addStringProperty("id", display.pin.id)
    addStringProperty("name", display.pin.name)
    addStringProperty("icon", display.icon)
    addStringProperty("kind", if (display.isDot) "dot" else "pin")
    addNumberProperty("size", display.size)
    addStringProperty("label", display.label ?: "")
    addBooleanProperty("active", display.pin.active)
    addNumberProperty(
      "rank",
      when {
        display.isDot -> 2
        display.pin.active -> 0
        else -> 1
      },
    )
  }

internal fun clientPinFeature(pin: MapPin): Feature =
  clientPinFeature(
    PinDisplay(
      pin = pin,
      position = pin.position,
      icon = pinImage(pin),
      size = 1f,
      isDot = false,
      label = pin.name,
    )
  )

internal fun pinImage(pin: MapPin): String =
  if (pin.active) MAP_PIN_ACTIVE_IMAGE else MAP_PIN_INACTIVE_IMAGE

internal fun dotImage(pin: MapPin): String =
  if (pin.active) MAP_PIN_DOT_ACTIVE_IMAGE else MAP_PIN_DOT_INACTIVE_IMAGE

private fun pointFeature(position: LatLng, icon: String): Feature =
  Feature.fromGeometry(Point.fromLngLat(position.longitude, position.latitude)).apply {
    addStringProperty("icon", icon)
  }

private fun emptyFeatureCollection(): FeatureCollection =
  FeatureCollection.fromFeatures(emptyList())

internal fun pinScale(
  nearestPx: Float,
  neighborCount: Int,
  collisionPx: Float,
): Float {
  if (neighborCount <= 1) return 1f
  val distanceScale = (nearestPx / collisionPx).coerceIn(PIN_MIN_SCALE, 1f)
  val densityScale =
    if (neighborCount <= 2) {
      1f
    } else {
      (1f / sqrt(neighborCount.toFloat())).coerceIn(PIN_MIN_SCALE, 1f)
    }
  return minOf(distanceScale, densityScale).coerceIn(PIN_MIN_SCALE, 1f)
}

internal fun computePinDisplays(
  pins: List<MapPin>,
  points: List<ScreenPoint>,
  selectedPinId: String?,
  density: Float,
  zoom: Double,
  unproject: (ScreenPoint) -> LatLng,
): List<PinDisplay> {
  require(pins.size == points.size) { "pins and points must have the same size" }
  if (pins.isEmpty()) return emptyList()

  val adjusted = points.toMutableList()
  if (zoom >= PIN_FAN_MIN_ZOOM) {
    pins.indices
      .groupBy { pinKey(pins[it].position) }
      .values
      .filter { it.size > 1 }
      .forEach { indices ->
        val centerX =
          indices.sumOf { points[it].x.toDouble() }.toFloat() / indices.size
        val centerY =
          indices.sumOf { points[it].y.toDouble() }.toFloat() / indices.size
        val radius =
          (PIN_FAN_BASE_DP + PIN_FAN_STEP_DP * (indices.size - 2)) * density
        indices.forEachIndexed { fanIndex, pinIndex ->
          val angle = (2.0 * PI * fanIndex / indices.size).toFloat()
          adjusted[pinIndex] =
            ScreenPoint(
              x = centerX + cos(angle) * radius,
              y = centerY + sin(angle) * radius,
            )
        }
      }
  }

  val collisionPx = PIN_COLLISION_RADIUS_DP * density
  return pins.mapIndexed { index, pin ->
    val selected = pin.id == selectedPinId
    val distances =
      adjusted.indices
        .filter { it != index }
        .map { distance(adjusted[index], adjusted[it]) }
    val nearest = distances.minOrNull() ?: Float.MAX_VALUE
    val neighborCount = 1 + distances.count { it <= collisionPx }
    val scale = if (selected) 1f else pinScale(nearest, neighborCount, collisionPx)
    val isDot = !selected && scale < PIN_DOT_SCALE

    PinDisplay(
      pin = pin,
      position =
        if (adjusted[index] == points[index]) pin.position else unproject(adjusted[index]),
      icon = if (isDot) dotImage(pin) else pinImage(pin),
      size = if (isDot) 1f else scale,
      isDot = isDot,
      label =
        when {
          selected -> pin.name
          !isDot && scale >= PIN_LABEL_SCALE -> pin.name
          else -> null
        },
    )
  }
}

internal fun MapLibreMap.pinDisplays(
  pins: List<MapPin>,
  selectedPinId: String?,
  density: Float,
): List<PinDisplay> {
  if (pins.isEmpty()) return emptyList()
  return runCatching {
    val points = pins.map { projection.toScreenLocation(it.position) }
    computePinDisplays(
      pins = pins,
      points = points.map { ScreenPoint(it.x, it.y) },
      selectedPinId = selectedPinId,
      density = density,
      zoom = cameraPosition.zoom,
      unproject = { point -> projection.fromScreenLocation(PointF(point.x, point.y)) },
    )
  }.getOrElse {
    pins.map { pin ->
      PinDisplay(
        pin = pin,
        position = pin.position,
        icon = pinImage(pin),
        size = 1f,
        isDot = false,
        label = pin.name,
      )
    }
  }
}

private fun distance(a: ScreenPoint, b: ScreenPoint): Float =
  hypot(a.x - b.x, a.y - b.y)

private fun pinKey(position: LatLng): Long =
  (position.latitude * 1_000_000).roundToLong() * 1_000_000L +
    (position.longitude * 1_000_000).roundToLong()

internal fun pinsSignature(pins: List<MapPin>): String =
  pins.joinToString("|") { pin ->
    "${pin.id}:${pin.name}:${pin.position.latitude}:${pin.position.longitude}:${pin.active}"
  }

internal sealed interface MapCameraTarget {
  data object None : MapCameraTarget

  data class Position(val position: LatLng, val zoom: Double) : MapCameraTarget

  data class Bounds(val bounds: LatLngBounds) : MapCameraTarget
}

internal fun pinsCameraTarget(pins: List<MapPin>): MapCameraTarget = when {
  pins.isEmpty() -> MapCameraTarget.None
  pins.size == 1 -> MapCameraTarget.Position(pins.first().position, MAP_DEFAULT_ZOOM)
  else -> MapCameraTarget.Bounds(
    LatLngBounds.Builder().includes(pins.map { it.position }).build(),
  )
}

internal fun mapCameraTarget(
  pins: List<MapPin>,
  userLocation: LatLng?,
  centeredOnUser: Boolean,
  hasFittedPins: Boolean,
): MapCameraTarget = when {
  userLocation != null && !centeredOnUser ->
    MapCameraTarget.Position(userLocation, MAP_USER_ZOOM)
  userLocation != null -> MapCameraTarget.None
  hasFittedPins -> MapCameraTarget.None
  pins.isEmpty() -> MapCameraTarget.Position(DEFAULT_CENTER, MAP_USER_ZOOM)
  else -> pinsCameraTarget(pins)
}

internal fun MapLibreMap.applyCameraTarget(target: MapCameraTarget) {
  when (target) {
    MapCameraTarget.None -> Unit
    is MapCameraTarget.Position ->
      moveCamera(CameraUpdateFactory.newLatLngZoom(target.position, target.zoom))
    is MapCameraTarget.Bounds ->
      moveCamera(CameraUpdateFactory.newLatLngBounds(target.bounds, 96))
  }
}

internal fun MapLibreMap.fitPins(pins: List<MapPin>) {
  applyCameraTarget(pinsCameraTarget(pins))
}

internal fun MapView.applyWhenLaidOut(action: () -> Unit) {
  if (width > 0 && height > 0) {
    action()
  } else {
    post {
      if (width > 0 && height > 0) action()
    }
  }
}

internal fun MapLibreMap.findClientPinAt(position: LatLng, radiusPx: Float): String? =
  runCatching {
    val screen = projection.toScreenLocation(position)
    val rect =
      RectF(
        screen.x - radiusPx,
        screen.y - radiusPx,
        screen.x + radiusPx,
        screen.y + radiusPx,
      )
    queryRenderedFeatures(rect, MAP_PINS_LAYER_ID, MAP_PIN_DOT_LAYER_ID)
      .minByOrNull { feature ->
        val point = feature.geometry() as? Point
        if (point == null) {
          Float.MAX_VALUE
        } else {
          val featureScreen =
            projection.toScreenLocation(LatLng(point.latitude(), point.longitude()))
          hypot(featureScreen.x - screen.x, featureScreen.y - screen.y)
        }
      }
      ?.getStringProperty("id")
  }.getOrNull()

@SuppressLint("ClickableViewAccessibility")
internal fun isWithinHitRadius(
  markerX: Float,
  markerY: Float,
  touchX: Float,
  touchY: Float,
  radiusPx: Float,
): Boolean {
  val dx = touchX - markerX
  val dy = touchY - markerY
  return dx * dx + dy * dy <= radiusPx * radiusPx
}

internal class DraggableMarkerController(
  private val mapView: MapView,
  private val mapProvider: () -> MapLibreMap?,
  private val positionProvider: () -> LatLng?,
  private val onMoved: (LatLng) -> Unit,
  private val onDragStarted: () -> Unit,
  private val onDragEnded: () -> Unit,
) : View.OnTouchListener {
  private val density = mapView.resources.displayMetrics.density
  private var dragging = false
  private var lastPosition: LatLng? = null

  fun attach() {
    mapView.setOnTouchListener(this)
  }

  fun detach() {
    mapView.setOnTouchListener(null)
  }

  override fun onTouch(view: View, event: MotionEvent): Boolean {
    val map = mapProvider() ?: return false
    return when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> startDrag(map, event)
      MotionEvent.ACTION_MOVE -> moveDrag(map, event)
      MotionEvent.ACTION_UP -> finishDrag(map)
      MotionEvent.ACTION_CANCEL -> cancelDrag(map)
      else -> false
    }
  }

  private fun startDrag(map: MapLibreMap, event: MotionEvent): Boolean {
    val position = positionProvider() ?: return false
    val screen = map.projection.toScreenLocation(position)
    val radius = HIT_RADIUS_DP * density
    if (!isWithinHitRadius(screen.x, screen.y, event.x, event.y, radius)) return false

    dragging = true
    lastPosition = position
    mapView.parent?.requestDisallowInterceptTouchEvent(true)
    onDragStarted()
    return true
  }

  private fun moveDrag(map: MapLibreMap, event: MotionEvent): Boolean {
    if (!dragging) return false
    if (event.pointerCount > 1) {
      cancelDrag(map)
      return false
    }
    val position = map.projection.fromScreenLocation(PointF(event.x, event.y))
    lastPosition = position
    onMoved(position)
    return true
  }

  private fun finishDrag(map: MapLibreMap): Boolean {
    if (!dragging) return false
    lastPosition?.let(onMoved)
    endDrag(map)
    return true
  }

  private fun cancelDrag(map: MapLibreMap): Boolean {
    if (!dragging) return false
    endDrag(map)
    return true
  }

  private fun endDrag(map: MapLibreMap) {
    dragging = false
    lastPosition = null
    mapView.parent?.requestDisallowInterceptTouchEvent(false)
    onDragEnded()
  }

  private companion object {
    const val HIT_RADIUS_DP = 48f
  }
}

// Deliberately swallows all touch input (non-interactive map preview), so it
// never needs to handle a click.
@SuppressLint("ClickableViewAccessibility")
internal class NonInteractiveMapView(
  context: Context,
  options: MapLibreMapOptions,
) : MapView(context, options) {
  override fun onTouchEvent(event: MotionEvent): Boolean = false

  override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
}
