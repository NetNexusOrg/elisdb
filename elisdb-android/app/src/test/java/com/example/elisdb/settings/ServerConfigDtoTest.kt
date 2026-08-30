package com.example.elisdb.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerConfigDtoTest {

  private val seed = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="

  private fun config(url: String) = """{"server_url":"$url","private_seed":"$seed"}"""

  @Test
  fun https_aceite() {
    val dto = parseServerConfig(config("https://db.example.com"))
    assertEquals("https://db.example.com", dto?.serverUrl)
  }

  @Test
  fun http_dominio_aceite() {
    val dto = parseServerConfig(config("http://db.example.com"))
    assertEquals("http://db.example.com", dto?.serverUrl)
  }

  @Test
  fun http_ip_aceite() {
    val dto = parseServerConfig(config("http://192.168.1.10"))
    assertEquals("http://192.168.1.10", dto?.serverUrl)
  }

  @Test
  fun esquema_nao_suportado_rejeitado() {
    assertNull(parseServerConfig(config("ftp://db.example.com")))
  }

  @Test
  fun url_sem_esquema_rejeitado() {
    assertNull(parseServerConfig(config("db.example.com")))
  }
}
