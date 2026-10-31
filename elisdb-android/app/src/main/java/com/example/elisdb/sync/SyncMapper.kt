package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.Foto
import com.example.elisdb.remote.ClienteInDto
import com.example.elisdb.remote.FotoInDto
import com.example.elisdb.remote.RecordDto

/** Maps local entities to the server transport DTOs. */
object SyncMapper {

  /**
   * Builds a record to push. Photos are referenced by id only; their binary
   * content is uploaded to the photo endpoint separately.
   */
  fun toRecord(cliente: Cliente, fotos: List<Foto>): RecordDto {
    val fotoDtos = fotos.map { FotoInDto(id = it.id, mime = it.mime, ordem = it.ordem) }
    return RecordDto(
      client =
        ClienteInDto(
          id = cliente.id,
          nome = cliente.nome,
          contrato = cliente.contrato,
          latitude = cliente.latitude,
          longitude = cliente.longitude,
          notas = cliente.notas,
          ativo = cliente.ativo,
          updatedAt = cliente.updatedAt,
        ),
      fotos = fotoDtos,
    )
  }
}
