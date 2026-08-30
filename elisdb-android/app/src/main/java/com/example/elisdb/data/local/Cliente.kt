package com.example.elisdb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class Cliente(
  @PrimaryKey val id: String,
  val nome: String,
  val contrato: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val notas: String? = null,
  val dataCriacao: Long,
  val updatedAt: Long,
  val createdAt: Long = 0,
  val pending: Boolean = false,
  val deletePending: Boolean = false,
)