package com.example.elisdb.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigDataCountTest {

  @Test
  fun `contagem desconhecida mostra travessao`() {
    assertEquals("—", dataCountLabel(null))
  }

  @Test
  fun `sem registos mostra travessao`() {
    assertEquals("—", dataCountLabel(0))
  }

  @Test
  fun `um registo mostra o numero`() {
    assertEquals("1", dataCountLabel(1))
  }

  @Test
  fun `varios registos mostram o numero`() {
    assertEquals("12", dataCountLabel(12))
  }
}
