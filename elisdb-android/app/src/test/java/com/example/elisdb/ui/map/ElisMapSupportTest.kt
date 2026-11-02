package com.example.elisdb.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.maplibre.android.geometry.LatLng

class ElisMapSupportTest {

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
    assertEquals(MAP_PIN_INACTIVE_IMAGE, inactive.getStringProperty("icon"))
  }
}
