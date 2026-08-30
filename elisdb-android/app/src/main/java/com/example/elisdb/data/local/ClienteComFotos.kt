package com.example.elisdb.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ClienteComFotos(
  @Embedded val cliente: Cliente,
  @Relation(parentColumn = "id", entityColumn = "clienteId") val fotos: List<Foto>,
)
