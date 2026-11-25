package com.example.elisdb.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServicoDto(
  @SerialName("item_id") val itemId: String,
  val quantidade: Int? = null,
)

@Serializable
data class ClienteInDto(
  val id: String,
  val nome: String,
  val contrato: String? = null,
  val tag: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val notas: String? = null,
  val ativo: Boolean = true,
  @SerialName("updated_at") val updatedAt: Long,
  /** Null preserves the server's associations; empty clears; a list replaces. */
  val servicos: List<ServicoDto>? = null,
)

@Serializable
data class FotoInDto(
  val id: String,
  val mime: String,
  val ordem: Int = 0,
  /**
   * True for the client's profile photo. Null only when a client that predates
   * this field pushes; the server then keeps the stored value.
   */
  val preferida: Boolean? = null,
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
  /**
   * Whether this is the client's profile photo. Null when an older server does
   * not know the field; the client then keeps its local choice.
   */
  val preferida: Boolean? = null,
  val hash: String? = null,
  /**
   * False when the server still knows the photo but has lost its bytes. The
   * client must keep any local copy instead of treating it as deleted.
   */
  val available: Boolean = true,
)

@Serializable
data class ClienteOutDto(
  val id: String,
  val nome: String,
  val contrato: String? = null,
  /** Absent on older servers, which store no tag. */
  val tag: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val notas: String? = null,
  /** Absent on older servers, which assume every client is active. */
  val ativo: Boolean = true,
  @SerialName("updated_at") val updatedAt: Long,
  @SerialName("created_at") val createdAt: Long? = null,
  /**
   * Cursor the server assigned to this version (pull watermark). Independent of
   * [updatedAt], which keeps deciding conflicts. Absent on older servers.
   */
  @SerialName("delivery_ts") val deliveryTs: Long? = null,
  /** Complete associations; null on old servers or the summary list. */
  val servicos: List<ServicoDto>? = null,
)

@Serializable
data class ClienteComFotosDto(
  val client: ClienteOutDto,
  val fotos: List<FotoOutDto> = emptyList(),
  /**
   * False when [fotos] may be partial. Older servers omit the field; the
   * conservative default prevents clients from deleting photos just because a
   * response did not list them.
   */
  @SerialName("photos_complete") val photosComplete: Boolean = false,
)

/** Server-side deletion marker returned by pull. */
@Serializable
data class TombstoneDto(
  val id: String,
  @SerialName("deleted_at") val deletedAt: Long,
  /** Delivery cursor of the deletion; independent of [deletedAt]. */
  @SerialName("delivery_ts") val deliveryTs: Long? = null,
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
  /**
   * Photos this push referenced that have no file on the server. They were not
   * stored, so the client must upload them again on the next push.
   */
  @SerialName("missing_photos") val missingPhotos: List<String> = emptyList(),
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

@Serializable
data class AuditServiceChangeDto(
  @SerialName("item_id") val itemId: String,
  val nome: String,
  val change: String,
  val old: Int? = null,
  val new: Int? = null,
)

/** What an edit changed; absent on entries recorded before change tracking. */
@Serializable
data class AuditDetailsDto(
  val fields: List<AuditFieldDto> = emptyList(),
  val photos: AuditPhotosDto? = null,
  val services: List<AuditServiceChangeDto> = emptyList(),
)

/** One entry of a client's edit history: who acted, when, and what changed. */
@Serializable
data class AuditEntryDto(
  val action: String,
  val username: String? = null,
  @SerialName("key_id") val keyId: String,
  val ts: Long,
  val details: AuditDetailsDto? = null,
  // Present on global activity entries; the per-client endpoint omits them.
  @SerialName("cliente_id") val clienteId: String? = null,
  @SerialName("cliente_nome") val clienteNome: String? = null,
  val kind: String = "client",
  @SerialName("item_id") val itemId: String? = null,
  @SerialName("item_nome") val itemNome: String? = null,
)

/** A page of history plus the cursor for the next page, when one exists. */
@Serializable
data class HistoryPageDto(
  val entries: List<AuditEntryDto> = emptyList(),
  @SerialName("next_after_id") val nextAfterId: Int? = null,
)

/** A page of the global activity log plus the cursor for older entries. */
@Serializable
data class ActivityPageDto(
  val entries: List<AuditEntryDto> = emptyList(),
  @SerialName("next_before_id") val nextBeforeId: Int? = null,
)
/** The Android package published by the server (`GET /api/app/latest`). */
@Serializable
data class AppReleaseDto(
  @SerialName("version_name") val versionName: String,
  @SerialName("version_code") val versionCode: Int,
  val size: Long,
  val sha256: String,
  val filename: String,
)

@Serializable
data class CatalogoItemDto(
  val id: String,
  val categoria: String,
  val nome: String,
  val ativo: Boolean = true,
  @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class CatalogoItemOutDto(
  val id: String,
  val categoria: String,
  val nome: String,
  val ativo: Boolean = true,
  @SerialName("created_at") val createdAt: Long? = null,
  @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class CatalogoPushRequestDto(val items: List<CatalogoItemDto> = emptyList())

@Serializable
data class CatalogoPushResultDto(
  val id: String,
  val status: String,
  val reason: String? = null,
  val item: CatalogoItemOutDto? = null,
)

@Serializable
data class CatalogoResponseDto(
  val items: List<CatalogoItemOutDto> = emptyList(),
  val complete: Boolean = true,
)
