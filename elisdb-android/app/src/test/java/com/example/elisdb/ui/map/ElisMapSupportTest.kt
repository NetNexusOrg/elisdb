package com.example.elisdb.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.geometry.LatLng

class ElisMapSupportTest {

  @Test
  fun `map style uses Liberty in both themes`() {
    assertEquals("https://tiles.openfreemap.org/styles/liberty", mapStyleUrl(isDark = false))
    assertEquals("https://tiles.openfreemap.org/styles/liberty", mapStyleUrl(isDark = true))
    assertEquals(MAP_STYLE_LIGHT_URL, MAP_STYLE_DARK_URL)
  }

  @Test
  fun `pins signature changes when position or state changes`() {
    val pin = MapPin("1", "Ana", LatLng(38.7223, -9.1393), active = true)

    assertEquals("1:Ana:38.7223:-9.1393:true", pinsSignature(listOf(pin)))
    assertNotEquals(
      pinsSignature(listOf(pin)),
      pinsSignature(listOf(pin.copy(active = false))),
    )
    assertNotEquals(
      pinsSignature(listOf(pin)),
      pinsSignature(listOf(pin.copy(position = LatLng(38.7, -9.1)))),
    )
  }

  @Test
  fun `hit radius accepts marker center and edge but rejects outside`() {
    assertEquals(true, isWithinHitRadius(100f, 100f, 100f, 100f, 48f))
    assertEquals(true, isWithinHitRadius(100f, 100f, 148f, 100f, 48f))
    assertEquals(false, isWithinHitRadius(100f, 100f, 149f, 100f, 48f))
  }

  @Test
  fun `pins camera target handles zero one and many pins`() {
    val pin = MapPin("1", "Ana", LatLng(38.7223, -9.1393))

    assertEquals(MapCameraTarget.None, pinsCameraTarget(emptyList()))
    assertEquals(
      MapCameraTarget.Position(pin.position, MAP_DEFAULT_ZOOM),
      pinsCameraTarget(listOf(pin)),
    )
    assertTrue(
      pinsCameraTarget(
        listOf(pin, MapPin("2", "Rui", LatLng(38.7, -9.1))),
      ) is MapCameraTarget.Bounds,
    )
  }

  @Test
  fun `camera target centers user and fits pins only once`() {
    val user = LatLng(38.72, -9.14)
    val pin = MapPin("1", "Ana", LatLng(38.7223, -9.1393))
    val pins = listOf(pin, MapPin("2", "Rui", LatLng(38.7, -9.1)))

    assertEquals(
      MapCameraTarget.Position(user, MAP_USER_ZOOM),
      mapCameraTarget(pins, user, centeredOnUser = false, hasFittedPins = false),
    )
    assertEquals(
      MapCameraTarget.None,
      mapCameraTarget(pins, user, centeredOnUser = true, hasFittedPins = false),
    )
    assertEquals(
      MapCameraTarget.None,
      mapCameraTarget(pins, userLocation = null, centeredOnUser = false, hasFittedPins = true),
    )
    assertEquals(
      MapCameraTarget.Position(DEFAULT_CENTER, MAP_USER_ZOOM),
      mapCameraTarget(emptyList(), null, centeredOnUser = false, hasFittedPins = false),
    )
    assertEquals(
      MapCameraTarget.Position(pin.position, MAP_DEFAULT_ZOOM),
      mapCameraTarget(
        listOf(pin),
        userLocation = null,
        centeredOnUser = false,
        hasFittedPins = false,
      ),
    )
    assertTrue(
      mapCameraTarget(
        pins,
        userLocation = null,
        centeredOnUser = false,
        hasFittedPins = false,
      ) is MapCameraTarget.Bounds,
    )
  }

  @Test
  fun `client pin feature carries id name and active icon`() {
    val active = clientPinFeature(
      MapPin("42", "Maria", LatLng(38.7223, -9.1393), active = true),
    )
    val inactive = clientPinFeature(
      MapPin("43", "João", LatLng(38.7223, -9.1393), active = false),
    )

    assertEquals("42", active.getStringProperty("id"))
    assertEquals("Maria", active.getStringProperty("name"))
    assertEquals(MAP_PIN_ACTIVE_IMAGE, active.getStringProperty("icon"))
    assertEquals("pin", active.getStringProperty("kind"))
    assertEquals("Maria", active.getStringProperty("label"))
    assertEquals(1f, active.getNumberProperty("size").toFloat(), 0.001f)
    assertEquals(MAP_PIN_INACTIVE_IMAGE, inactive.getStringProperty("icon"))
  }

  @Test
  fun `pin scale shrinks as neighbours get closer`() {
    assertEquals(1f, pinScale(Float.MAX_VALUE, neighborCount = 1, collisionPx = 34f), 0.001f)
    assertEquals(1f, pinScale(40f, neighborCount = 2, collisionPx = 34f), 0.001f)
    assertEquals(
      PIN_MIN_SCALE,
      pinScale(0f, neighborCount = 2, collisionPx = 34f),
      0.001f,
    )
    assertTrue(pinScale(17f, neighborCount = 3, collisionPx = 34f) < PIN_DOT_SCALE)
  }

  @Test
  fun `pin displays keep full pins when far apart`() {
    val pins =
      listOf(
        MapPin("1", "Ana", LatLng(0.0, 0.0)),
        MapPin("2", "Rui", LatLng(0.0, 1.0)),
      )
    val displays =
      computePinDisplays(
        pins = pins,
        points = listOf(ScreenPoint(0f, 0f), ScreenPoint(100f, 0f)),
        selectedPinId = null,
        density = 1f,
        zoom = 15.0,
        unproject = { point -> LatLng(point.y.toDouble(), point.x.toDouble()) },
      )

    assertEquals(1f, displays[0].size, 0.001f)
    assertFalse(displays[0].isDot)
    assertEquals("Ana", displays[0].label)
    assertEquals("Rui", displays[1].label)
  }

  @Test
  fun `pin displays shrink to dots when close`() {
    val pins =
      listOf(
        MapPin("1", "Ana", LatLng(0.0, 0.0)),
        MapPin("2", "Rui", LatLng(0.0, 1.0)),
      )
    val displays =
      computePinDisplays(
        pins = pins,
        points = listOf(ScreenPoint(0f, 0f), ScreenPoint(10f, 0f)),
        selectedPinId = null,
        density = 1f,
        zoom = 15.0,
        unproject = { point -> LatLng(point.y.toDouble(), point.x.toDouble()) },
      )

    assertEquals(1f, displays[0].size, 0.001f)
    assertTrue(displays[0].isDot)
    assertNull(displays[0].label)
  }

  @Test
  fun `selected pin keeps full size and label`() {
    val pins =
      listOf(
        MapPin("1", "Ana", LatLng(0.0, 0.0)),
        MapPin("2", "Rui", LatLng(0.0, 1.0)),
      )
    val displays =
      computePinDisplays(
        pins = pins,
        points = listOf(ScreenPoint(0f, 0f), ScreenPoint(10f, 0f)),
        selectedPinId = "1",
        density = 1f,
        zoom = 15.0,
        unproject = { point -> LatLng(point.y.toDouble(), point.x.toDouble()) },
      )

    assertEquals(1f, displays[0].size, 0.001f)
    assertFalse(displays[0].isDot)
    assertEquals("Ana", displays[0].label)
  }

  @Test
  fun `exact duplicate positions fan out on screen`() {
    val pins =
      listOf(
        MapPin("1", "Ana", LatLng(0.0, 0.0)),
        MapPin("2", "Rui", LatLng(0.0, 0.0)),
      )
    val displays =
      computePinDisplays(
        pins = pins,
        points = listOf(ScreenPoint(10f, 10f), ScreenPoint(10f, 10f)),
        selectedPinId = null,
        density = 1f,
        zoom = 15.0,
        unproject = { point -> LatLng(point.y / 1000.0, point.x / 1000.0) },
      )

    assertNotEquals(displays[0].position.longitude, displays[1].position.longitude, 1e-9)
  }
}
