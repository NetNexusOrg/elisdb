package com.example.elisdb.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalUriHandler
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
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

internal const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
internal const val MAP_DEFAULT_ZOOM = 16.0
internal const val MAP_USER_ZOOM = 15.0
internal const val MAP_PINS_SOURCE_ID = "client-pins"
internal const val MAP_PINS_LAYER_ID = "client-pins-layer"
internal const val MAP_CLUSTER_LAYER_ID = "client-clusters"
internal const val MAP_CLUSTER_COUNT_LAYER_ID = "client-cluster-count"
internal const val MAP_PICKED_SOURCE_ID = "picked-location"
internal const val MAP_PICKED_LAYER_ID = "picked-location-layer"
internal const val MAP_USER_SOURCE_ID = "user-location"
internal const val MAP_USER_LAYER_ID = "user-location-layer"
internal const val MAP_PIN_ACTIVE_IMAGE = "pin-active"
internal const val MAP_PIN_INACTIVE_IMAGE = "pin-inactive"
internal const val MAP_USER_LOCATION_IMAGE = "user-location"

internal val DEFAULT_CENTER = LatLng(38.7223, -9.1393)

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
internal fun rememberMapStyle(mapView: MapView): MapStyleState {
  var style by remember(mapView) { mutableStateOf<Style?>(null) }
  var failed by remember(mapView) { mutableStateOf(false) }
  var attempt by remember(mapView) { mutableIntStateOf(0) }
  val retry =
    remember(mapView) {
      {
        style = null
        failed = false
        attempt += 1
      }
    }

  DisposableEffect(mapView, attempt) {
    var disposed = false
    val failureListener =
      MapView.OnDidFailLoadingMapListener {
        if (!disposed) failed = true
      }
    mapView.addOnDidFailLoadingMapListener(failureListener)
    mapView.getMapAsync { map ->
      if (disposed) return@getMapAsync
      map.setStyle(MAP_STYLE_URL) { loadedStyle ->
        if (!disposed) {
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
internal fun rememberMapRenderedState(mapView: MapView): MutableState<Boolean> {
  val rendered = remember(mapView) { mutableStateOf(false) }
  DisposableEffect(mapView) {
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

@Composable
internal fun MapAttribution(modifier: Modifier = Modifier) {
  var showDialog by remember { mutableStateOf(false) }
  val uriHandler = LocalUriHandler.current

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopEnd,
  ) {
    Text(
      text = "© OpenStreetMap",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .padding(4.dp)
        .background(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
          shape = RoundedCornerShape(4.dp),
        )
        .clickable { showDialog = true }
        .padding(horizontal = 6.dp, vertical = 3.dp),
    )
  }

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = { Text("Atribuição do mapa") },
      text = {
        Column {
          Text("© OpenStreetMap contributors")
          Text("Dados: OpenFreeMap / OpenMapTiles")
          Text("MapLibre Native")
          TextButton(
            onClick = { uriHandler.openUri("https://www.openstreetmap.org/copyright") },
          ) { Text("OpenStreetMap") }
          TextButton(
            onClick = { uriHandler.openUri("https://openfreemap.org") },
          ) { Text("OpenFreeMap") }
          TextButton(
            onClick = { uriHandler.openUri("https://openmaptiles.org") },
          ) { Text("OpenMapTiles") }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDialog = false }) { Text("Fechar") }
      },
    )
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
  if (getImage(MAP_USER_LOCATION_IMAGE) == null) {
    addImage(
      MAP_USER_LOCATION_IMAGE,
      requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_user_location)),
    )
  }
}

internal fun Style.addClientPinsLayer(pins: List<MapPin> = emptyList()) {
  if (getSourceAs<GeoJsonSource>(MAP_PINS_SOURCE_ID) == null) {
    val options = GeoJsonOptions()
      .withCluster(true)
      .withClusterRadius(50)
      .withClusterMaxZoom(16)
    addSource(
      GeoJsonSource(
        MAP_PINS_SOURCE_ID,
        FeatureCollection.fromFeatures(pins.map(::clientPinFeature)),
        options,
      )
    )
  }
  if (getLayerAs<CircleLayer>(MAP_CLUSTER_LAYER_ID) == null) {
    addLayer(
      CircleLayer(MAP_CLUSTER_LAYER_ID, MAP_PINS_SOURCE_ID)
        .withProperties(
          PropertyFactory.circleColor("#4051B5"),
          PropertyFactory.circleRadius(18f),
          PropertyFactory.circleStrokeColor("#FFFFFF"),
          PropertyFactory.circleStrokeWidth(2f),
        )
        .withFilter(Expression.has("point_count"))
    )
  }
  if (getLayerAs<SymbolLayer>(MAP_CLUSTER_COUNT_LAYER_ID) == null) {
    addLayer(
      SymbolLayer(MAP_CLUSTER_COUNT_LAYER_ID, MAP_PINS_SOURCE_ID)
        .withProperties(
          PropertyFactory.textField(
            Expression.toString(Expression.get("point_count")),
          ),
          PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
          PropertyFactory.textSize(12f),
          PropertyFactory.textColor("#FFFFFF"),
          PropertyFactory.textAllowOverlap(true),
          PropertyFactory.textIgnorePlacement(true),
        )
        .withFilter(Expression.has("point_count"))
    )
  }
  if (getLayerAs<SymbolLayer>(MAP_PINS_LAYER_ID) == null) {
    addLayer(
      SymbolLayer(MAP_PINS_LAYER_ID, MAP_PINS_SOURCE_ID)
        .withProperties(
          *pinLayerProperties(),
        )
        .withFilter(Expression.has("id"))
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
    PropertyFactory.iconSize(1f),
    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
    PropertyFactory.iconAllowOverlap(true),
    PropertyFactory.iconIgnorePlacement(true),
    PropertyFactory.textField(Expression.get("name")),
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

internal fun Style.updateClientPins(pins: List<MapPin>) {
  getSourceAs<GeoJsonSource>(MAP_PINS_SOURCE_ID)?.setGeoJson(
    FeatureCollection.fromFeatures(pins.map(::clientPinFeature)),
  )
}

internal fun clientPinFeature(pin: MapPin): Feature =
  Feature.fromGeometry(Point.fromLngLat(pin.position.longitude, pin.position.latitude)).apply {
    addStringProperty("id", pin.id)
    addStringProperty("name", pin.name)
    addStringProperty(
      "icon",
      if (pin.active) MAP_PIN_ACTIVE_IMAGE else MAP_PIN_INACTIVE_IMAGE,
    )
    addBooleanProperty("active", pin.active)
    addNumberProperty("rank", if (pin.active) 0 else 1)
  }

private fun pointFeature(position: LatLng, icon: String): Feature =
  Feature.fromGeometry(Point.fromLngLat(position.longitude, position.latitude)).apply {
    addStringProperty("icon", icon)
  }

private fun emptyFeatureCollection(): FeatureCollection =
  FeatureCollection.fromFeatures(emptyList())

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
    queryRenderedFeatures(rect, MAP_PINS_LAYER_ID)
      .firstOrNull()
      ?.getStringProperty("id")
  }.getOrNull()

internal data class ClusterHit(
  val feature: Feature,
  val id: Long,
  val pointCount: Int,
)

internal fun MapLibreMap.findClientClusterAt(position: LatLng, radiusPx: Float): ClusterHit? =
  runCatching {
    val screen = projection.toScreenLocation(position)
    val rect =
      RectF(
        screen.x - radiusPx,
        screen.y - radiusPx,
        screen.x + radiusPx,
        screen.y + radiusPx,
      )
    val feature =
      queryRenderedFeatures(rect, MAP_CLUSTER_LAYER_ID).firstOrNull()
        ?: return@runCatching null
    val id = feature.getNumberProperty("cluster_id")?.toLong() ?: return@runCatching null
    val count = feature.getNumberProperty("point_count")?.toInt() ?: 0
    ClusterHit(feature, id, count)
  }.getOrNull()

internal fun MapLibreMap.expandCluster(hit: ClusterHit) {
  val source = getStyle()?.getSourceAs<GeoJsonSource>(MAP_PINS_SOURCE_ID) ?: return
  val point = hit.feature.geometry() as? Point ?: return
  val target = LatLng(point.latitude(), point.longitude())
  val expansionZoom =
    runCatching { source.getClusterExpansionZoom(hit.feature) }.getOrNull()
      ?: (cameraPosition.zoom + 2.0).toInt()
  animateCamera(
    CameraUpdateFactory.newLatLngZoom(
      target,
      expansionZoom.toDouble().coerceAtLeast(cameraPosition.zoom + 1.0),
    )
  )
}

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
