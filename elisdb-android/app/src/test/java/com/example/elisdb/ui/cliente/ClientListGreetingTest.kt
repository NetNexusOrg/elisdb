package com.example.elisdb.ui.cliente

import org.junit.Assert.assertEquals
import org.junit.Test

class ClientListGreetingTest {

  @Test
  fun saudacaoDependeDaHora() {
    assertEquals("Bom dia, Ricardo", greetingFor(5, "Ricardo"))
    assertEquals("Bom dia, Ricardo", greetingFor(11, "Ricardo"))
    assertEquals("Boa tarde, Ricardo", greetingFor(12, "Ricardo"))
    assertEquals("Boa tarde, Ricardo", greetingFor(19, "Ricardo"))
    assertEquals("Boa noite, Ricardo", greetingFor(20, "Ricardo"))
    assertEquals("Boa noite, Ricardo", greetingFor(23, "Ricardo"))
    assertEquals("Boa noite, Ricardo", greetingFor(0, "Ricardo"))
    assertEquals("Boa noite, Ricardo", greetingFor(4, "Ricardo"))
  }

  @Test
  fun semNomeMostraApenasASaudacao() {
    assertEquals("Bom dia", greetingFor(9, ""))
    assertEquals("Boa tarde", greetingFor(15, "   "))
  }

  @Test
  fun nomeEEspacosSaoRemovidos() {
    assertEquals("Boa noite, Ana", greetingFor(21, "  Ana  "))
  }
}
