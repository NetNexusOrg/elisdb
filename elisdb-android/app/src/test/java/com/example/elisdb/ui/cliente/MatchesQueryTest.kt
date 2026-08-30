package com.example.elisdb.ui.cliente

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class MatchesQueryTest {

  private fun cliente(
    nome: String = "Maria",
    contrato: String? = null,
    notas: String? = null,
  ) =
    ClienteComFotos(
      cliente = Cliente(id = "1", nome = nome, contrato = contrato, notas = notas, latitude = 0.0, longitude = 0.0, dataCriacao = 0, updatedAt = 0),
      fotos = emptyList(),
    )

  @Test
  fun matches_porNome() {
    assertTrue(matchesQuery(cliente(nome = "Maria"), "ma"))
  }

  @Test
  fun matches_porContrato() {
    assertTrue(matchesQuery(cliente(contrato = "CT-2026-01"), "ct-2026"))
  }

  @Test
  fun matches_porNotas() {
    assertTrue(matchesQuery(cliente(notas = "Cliente prioritário"), "priorit"))
  }

  @Test
  fun matches_ignoraMaiusculasMinusculas() {
    assertTrue(matchesQuery(cliente(nome = "Maria"), "MARIA"))
  }

  @Test
  fun matches_ignoraAcentos() {
    assertTrue(matchesQuery(cliente(nome = "João"), "joao"))
  }

  @Test
  fun queryVaziaCorrespondeATudo() {
    assertTrue(matchesQuery(cliente(), ""))
    assertTrue(matchesQuery(cliente(), "   "))
  }

  @Test
  fun semCorrespondenciaRetornaFalse() {
    assertFalse(matchesQuery(cliente(nome = "Maria", contrato = "CT-2026-01"), "xyz"))
  }
}
