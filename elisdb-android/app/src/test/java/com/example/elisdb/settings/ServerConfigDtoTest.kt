package com.example.elisdb.settings

import org.junit.Assert.assertEquals
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
  fun http_publico_e_parseado_e_a_politica_e_aplicada_no_import() {
    assertEquals("http://db.example.com", parseServerConfig(config("http://db.example.com"))?.serverUrl)
  }

  @Test
  fun http_localhost_aceite() {
    val dto = parseServerConfig(config("http://localhost:8000"))
    assertEquals("http://localhost:8000", dto?.serverUrl)
  }

  @Test
  fun http_ip_privado_aceite() {
    val dto = parseServerConfig(config("http://192.168.1.10"))
    assertEquals("http://192.168.1.10", dto?.serverUrl)
  }

  @Test
  fun http_ipv6_loopback_aceite() {
    val dto = parseServerConfig(config("http://[::1]:8000"))
    assertEquals("http://[::1]:8000", dto?.serverUrl)
  }

  @Test
  fun esquema_nao_suportado_e_parseado_e_rejeitado_no_import() {
    assertEquals("ftp://db.example.com", parseServerConfig(config("ftp://db.example.com"))?.serverUrl)
  }

  @Test
  fun url_sem_esquema_e_parseada_e_rejeitada_no_import() {
    assertEquals("db.example.com", parseServerConfig(config("db.example.com"))?.serverUrl)
  }
}
