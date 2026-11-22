package com.example.elisdb.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteDtoTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `client history entry decodes without the global fields`() {
    val entry =
      json.decodeFromString<AuditEntryDto>(
        """{"action":"edit","username":"Maria","key_id":"k1","ts":100}"""
      )

    assertEquals("edit", entry.action)
    assertNull(entry.clienteId)
    assertNull(entry.clienteNome)
  }

  @Test
  fun `global history page decodes the client and the next cursor`() {
    val page =
      json.decodeFromString<ActivityPageDto>(
        """
        {"entries":[
          {"action":"create","username":"Maria","key_id":"k1","ts":100,
           "cliente_id":"c1","cliente_nome":"ANA"}
        ],"next_before_id":42}
        """
          .trimIndent()
      )

    assertEquals("ANA", page.entries.single().clienteNome)
    assertEquals("c1", page.entries.single().clienteId)
    assertEquals(42, page.nextBeforeId)
  }

  @Test
  fun `last global history page decodes without a cursor`() {
    val page = json.decodeFromString<ActivityPageDto>("""{"entries":[]}""")

    assertEquals(0, page.entries.size)
    assertNull(page.nextBeforeId)
  }
}
