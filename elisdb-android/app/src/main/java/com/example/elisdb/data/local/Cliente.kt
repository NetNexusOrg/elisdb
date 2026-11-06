package com.example.elisdb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class Cliente(
  @PrimaryKey val id: String,
  val nome: String,
  val contrato: String? = null,
  /** Optional free-text label used to group clients (filtering comes later). */
  val tag: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val notas: String? = null,
  /** Inactive clients are kept (and synced) but marked as out of service. */
  val ativo: Boolean = true,
  val dataCriacao: Long,
  val updatedAt: Long,
  val createdAt: Long = 0,
  val pending: Boolean = false,
  val deletePending: Boolean = false,
  /**
   * True when a completed full pull did not find this record on the server.
   * The record is kept (never deleted automatically) and surfaced in the UI as
   * "local only"; the user decides whether to replace local data with the
   * server dataset.
   */
  val localOnly: Boolean = false,
  /**
   * Monotonic local revision, bumped on every local write. The push
   * acknowledgement compares it instead of [updatedAt], which only has
   * one-second resolution: an edit made in the same second as the pushed
   * snapshot would otherwise be cleared as if it had already been sent.
   */
  val revision: Long = 0,
)