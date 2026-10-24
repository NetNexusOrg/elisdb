package com.example.elisdb.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClienteInDto(
  val id: String,
  val nome: String,
  val contrato: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val notas: String? = null,
  @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class FotoInDto(
  val id: String,
  val mime: String,
  val ordem: Int = 0,
)

@Serializable
data class RecordDto(
  val client: ClienteInDto,
  val fotos: List<FotoInDto> = emptyList(),
)

@Serializable
data class PushRequestDto(
  val records: List<RecordDto> = emptyList(),
)

@Serializable
data class FotoOutDto(
  val id: String,
  val mime: String,
  val ordem: Int = 0,
  val hash: String? = null,
)

@Serializable
data class ClienteOutDto(
  val id: String,
  val nome: String,
  val contrato: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val notas: String? = null,
  @SerialName("updated_at") val updatedAt: Long,
  @SerialName("created_at") val createdAt: Long? = null,
)

@Serializable
data class ClienteComFotosDto(
  val client: ClienteOutDto,
  val fotos: List<FotoOutDto> = emptyList(),
)

/** Server-side deletion marker returned by pull. */
@Serializable
data class TombstoneDto(
  val id: String,
  @SerialName("deleted_at") val deletedAt: Long,
)

/** Envelope returned by the pull endpoint (replaces the historical flat list). */
@Serializable
data class PullResponseDto(
  val records: List<ClienteComFotosDto> = emptyList(),
  val tombstones: List<TombstoneDto> = emptyList(),
  @SerialName("next_since") val nextSince: Long? = null,
  @SerialName("next_since_id") val nextSinceId: String? = null,
)

@Serializable
data class PushResultDto(
  val id: String,
  val status: String,
  @SerialName("existing_id") val existingId: String? = null,
  /** Canonical record (client + photos) on a "duplicate" response; null otherwise. */
  val client: ClienteOutDto? = null,
  val fotos: List<FotoOutDto> = emptyList(),
)

/** Identity lookup response for `GET /api/keys/me`: the key holder's allowlist record. */
@Serializable
data class KeyIdentityDto(
  @SerialName("key_id") val keyId: String,
  val username: String? = null,
)

/** One field changed by an edit, with the values before and after. */
@Serializable
data class AuditFieldDto(
  val field: String,
  val old: String? = null,
  val new: String? = null,
  val truncated: Boolean = false,
)

/** Photos added, removed or reordered by an edit. */
@Serializable
data class AuditPhotosDto(
  val added: List<String> = emptyList(),
  val removed: List<String> = emptyList(),
  val reordered: Boolean = false,
)

/** What an edit changed; absent on entries recorded before change tracking. */
@Serializable
data class AuditDetailsDto(
  val fields: List<AuditFieldDto> = emptyList(),
  val photos: AuditPhotosDto? = null,
)

/** One entry of a client's edit history: who acted, when, and what changed. */
@Serializable
data class AuditEntryDto(
  val action: String,
  val username: String? = null,
  @SerialName("key_id") val keyId: String,
  val ts: Long,
  val details: AuditDetailsDto? = null,
)