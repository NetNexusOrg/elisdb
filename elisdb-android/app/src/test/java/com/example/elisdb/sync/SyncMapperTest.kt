package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.Foto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMapperTest {

  @Test
  fun toRecord_enviaEstadoInativo() {
    val record = SyncMapper.toRecord(cliente(ativo = false), fotos = emptyList())
    assertFalse(record.client.ativo)
  }

  @Test
  fun toRecord_enviaEstadoAtivo() {
    val record = SyncMapper.toRecord(cliente(ativo = true), fotos = emptyList())
    assertTrue(record.client.ativo)
  }

  @Test
  fun toRecord_enviaFotoDePerfil() {
    val record =
      SyncMapper.toRecord(
        cliente(ativo = true),
        fotos = listOf(Foto(id = "1", clienteId = "1", uriLocal = "/tmp/a.webp", preferida = true)),
      )
    assertTrue(record.fotos.single().preferida == true)
  }

  private fun cliente(ativo: Boolean) =
    Cliente(
      id = "1",
      nome = "MARIA",
      ativo = ativo,
      dataCriacao = 100,
      updatedAt = 200,
    )
}
