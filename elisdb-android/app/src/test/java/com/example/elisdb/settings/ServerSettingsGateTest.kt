package com.example.elisdb.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerSettingsGateTest {

  @Test
  fun `sem url nao tem credenciais`() {
    val settings = ServerSettings(baseUrl = "", signingKey = "seed", userName = "maria")

    assertFalse(settings.hasCredentials())
    assertFalse(settings.hasConfiguredUser())
  }

  @Test
  fun `sem chave nao tem credenciais`() {
    val settings = ServerSettings(baseUrl = "https://s", signingKey = "", userName = "maria")

    assertFalse(settings.hasCredentials())
    assertFalse(settings.hasConfiguredUser())
  }

  @Test
  fun `url e chave sem nome nao completam o utilizador`() {
    val settings = ServerSettings(baseUrl = "https://s", signingKey = "seed", userName = "")

    assertTrue(settings.hasCredentials())
    assertFalse(settings.hasConfiguredUser())
  }

  @Test
  fun `nome em branco nao completa o utilizador`() {
    val settings = ServerSettings(baseUrl = "https://s", signingKey = "seed", userName = "   ")

    assertTrue(settings.hasCredentials())
    assertFalse(settings.hasConfiguredUser())
  }

  @Test
  fun `configuracao completa passa o gate`() {
    val settings = ServerSettings(baseUrl = "https://s", signingKey = "seed", userName = "maria")

    assertTrue(settings.hasCredentials())
    assertTrue(settings.hasConfiguredUser())
  }
}
