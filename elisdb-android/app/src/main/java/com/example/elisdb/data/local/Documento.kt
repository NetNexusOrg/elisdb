package com.example.elisdb.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An attachment of the client's "Mapa de dispositivos": an image or a PDF.
 * Files live in the same directory as the photos, but the two kinds never share
 * a list in the UI (a PDF must not reach the photo viewer).
 */
@Entity(
  tableName = "documentos",
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
data class Documento(
  @PrimaryKey val id: String,
  val clienteId: String,
  val nome: String = "",
  val mime: String = "application/pdf",
  val uriLocal: String,
  val ordem: Int = 0,
  val deletePending: Boolean = false,
)
