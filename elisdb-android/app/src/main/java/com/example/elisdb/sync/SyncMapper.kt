package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteServico
import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.ClienteInDto
import com.example.elisdb.remote.FotoInDto
import com.example.elisdb.remote.RecordDto
import com.example.elisdb.remote.ServicoDto

/** Maps local entities to the server transport DTOs. */
object SyncMapper {

  /**
   * Builds a record to push. Photos are referenced by id only; their binary
   * content is uploaded to the photo endpoint separately.
   */
  fun toRecord(
    cliente: Cliente,
    fotos: List<Foto>,
    servicos: List<ClienteServico> = emptyList(),
  ): RecordDto {
    val fotoDtos =
      fotos.map {
        FotoInDto(id = it.id, mime = it.mime, ordem = it.ordem, preferida = it.preferida)
      }
    return RecordDto(
      client =
        ClienteInDto(
          id = cliente.id,
          nome = cliente.nome,
          contrato = cliente.contrato,
          tag = cliente.tag,
          latitude = cliente.latitude,
          longitude = cliente.longitude,
          notas = cliente.notas,
          ativo = cliente.ativo,
          updatedAt = cliente.updatedAt,
          servicos = servicos.map {
            ServicoDto(itemId = it.itemId, quantidade = it.quantidade)
          },
        ),
      fotos = fotoDtos,
    )
  }
}
