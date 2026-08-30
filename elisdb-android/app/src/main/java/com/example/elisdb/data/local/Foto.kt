package com.example.elisdb.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "fotos",
  foreignKeys =
    [
      ForeignKey(
        entity = Cliente::class,
        parentColumns = ["id"],
        childColumns = ["clienteId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("clienteId")],
)
data class Foto(
  @PrimaryKey val id: String,
  val clienteId: String,
  val uriLocal: String,
  val mime: String = "image/webp",
  val ordem: Int = 0,
  val deletePending: Boolean = false,
)