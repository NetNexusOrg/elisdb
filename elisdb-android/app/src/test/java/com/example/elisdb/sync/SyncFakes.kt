package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteDao
import com.example.elisdb.data.local.Foto
import com.example.elisdb.data.local.FotoDao
import com.example.elisdb.remote.ClienteComFotosDto
import com.example.elisdb.remote.ClienteOutDto
import com.example.elisdb.remote.FotoOutDto
import com.example.elisdb.remote.HistoryPageDto
import com.example.elisdb.remote.PullResponseDto
import com.example.elisdb.remote.PushResultDto
import com.example.elisdb.remote.RecordDto
import com.example.elisdb.remote.ServerException
import com.example.elisdb.remote.TombstoneDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** In-memory fake [ClienteDao]. */
class FakeClienteDao(initial: List<Cliente> = emptyList()) : ClienteDao {  val data = initial.toMutableList()

  override fun observeAll(): Flow<List<Cliente>> = flowOf(data.filter { !it.deletePending })
  override fun observeAllIncludingDeleted(): Flow<List<Cliente>> = flowOf(data.toList())
  override fun observeCount(): Flow<Int> = flowOf(data.count { !it.deletePending })

  override fun observeLocalOnlyCount(): Flow<Int> =
    flowOf(data.count { it.localOnly && !it.deletePending })

  override suspend fun getAll(): List<Cliente> = data.toList()

  override fun observeAllComFotos(): Flow<List<com.example.elisdb.data.local.ClienteComFotos>> =
    flowOf(emptyList())

  override fun observeClienteComFotos(id: String): Flow<com.example.elisdb.data.local.ClienteComFotos?> =
    flowOf(null)

  override suspend fun getClienteComFotos(id: String): com.example.elisdb.data.local.ClienteComFotos? = null

  override suspend fun getById(id: String): Cliente? = data.find { it.id == id }

  override suspend fun getPending(): List<Cliente> = data.filter { it.pending }

  override suspend fun getPendingDeletions(): List<Cliente> = data.filter { it.deletePending }

  override suspend fun insert(cliente: Cliente) {
    data.removeAll { it.id == cliente.id }
    data.add(cliente)
  }

  override suspend fun upsert(cliente: Cliente) {
    data.removeAll { it.id == cliente.id }
    data.add(cliente)
  }

  override suspend fun update(cliente: Cliente) {
    val i = data.indexOfFirst { it.id == cliente.id }
    if (i >= 0) data[i] = cliente else data.add(cliente)
  }

  override suspend fun markSynced(id: String, seenRevision: Long): Int {
    val i = data.indexOfFirst { it.id == id }
    if (i < 0) return 0
    if (data[i].revision != seenRevision) return 0
    data[i] = data[i].copy(pending = false)
    return 1
  }

  override suspend fun delete(cliente: Cliente) {
    data.removeAll { it.id == cliente.id }
  }
}

/** Fake [FotoDao] keyed by clienteId. */
class FakeFotoDao(initial: List<Foto> = emptyList()) : FotoDao {
  val data = initial.toMutableList()

  override fun observeByCliente(clienteId: String): Flow<List<Foto>> =
    flowOf(data.filter { it.clienteId == clienteId && !it.deletePending })

  override suspend fun getByCliente(clienteId: String): List<Foto> =
    data.filter { it.clienteId == clienteId }

  override suspend fun insert(foto: Foto) {
    data.removeAll { it.id == foto.id }
    data.add(foto)
  }

  override suspend fun deleteByIds(ids: List<String>) {
    data.removeAll { it.id in ids }
  }

  override suspend fun update(foto: Foto) {
    val i = data.indexOfFirst { it.id == foto.id }
    if (i >= 0) data[i] = foto else data.add(foto)
  }

  override suspend fun getAllUris(): List<String> = data.map { it.uriLocal }
}

/** In-memory [PhotoStore]. */
class FakePhotoStore : PhotoStore {
  val stored = mutableListOf<String>()
  val files = mutableSetOf<String>()
  val content = mutableMapOf<String, ByteArray>()

  var storeCalls = 0
  var failStore = false

  /** Registers an existing file with its base64 content, mirroring the disk. */
  fun stage(uri: String, dataB64: String) {
    files.add(uri)
    content[uri] = decoded(dataB64)
  }

  /** Base64 of the stored bytes at [uri], excluding the test JPEG marker. */
  fun contentB64(uri: String): String {
    val bytes = content[uri]!!
    val payload =
      if (bytes.size >= 3 &&
        bytes[0] == 0xFF.toByte() &&
        bytes[1] == 0xD8.toByte() &&
        bytes[2] == 0xFF.toByte()
      ) {
        bytes.copyOfRange(3, bytes.size)
      } else {
        bytes
      }
    return java.util.Base64.getEncoder().encodeToString(payload)
  }

  override suspend fun readBytes(uriLocal: String): ByteArray? = content[uriLocal]

  override suspend fun storeBytes(bytes: ByteArray, mime: String): String {
    storeCalls++
    if (failStore) throw java.io.IOException("store failed")
    val extension = FilePhotoStore.extensionFor(mime)
    val path = "file:/foto_${stored.size}.$extension"
    stored.add(path)
    files.add(path)
    content[path] = bytes
    return path
  }

  override suspend fun contentHashMatches(uriLocal: String, hash: String): Boolean =
    content[uriLocal]?.let { PhotoHashes.sha256(it) == hash } ?: false

  override suspend fun delete(uriLocal: String) {
    stored.remove(uriLocal)
    files.remove(uriLocal)
    content.remove(uriLocal)
  }

  override suspend fun listFiles(): List<String> = files.toList()
}

/** In-memory [PullWatermark]. */
class FakePullWatermark(var value: Long? = null) : PullWatermark {
  /** Number of times the watermark was cleared, for assertions. */
  var clears = 0

  override suspend fun get(): Long? = value
  override suspend fun set(value: Long) {
    this.value = value
  }

  override suspend fun clear() {
    clears++
    value = null
  }
}

/** In-memory [ClockOffsetStore]. */
class FakeClockOffsetStore(var offset: Long? = null) : ClockOffsetStore {
  override fun getOffsetSeconds(): Long? = offset
  override fun setOffsetSeconds(offsetSeconds: Long) {
    offset = offsetSeconds
  }

  override fun clear() {
    offset = null
  }
}

/** In-memory [PhotoPushState]. */
class FakePhotoPushState : PhotoPushState {
  val hashes = mutableMapOf<String, Map<String, String>>()
  override fun getHashes(clienteId: String): Map<String, String> = hashes[clienteId] ?: emptyMap()
  override fun setHashes(clienteId: String, hashes: Map<String, String>) {
    this.hashes[clienteId] = hashes
  }

  override fun clear(clienteId: String) {
    hashes.remove(clienteId)
  }

  override fun clearAll() {
    hashes.clear()
  }
}

/** Fake network transport controlled by the test. */
class FakeSyncApi : SyncApi {
  var pushResult: (List<RecordDto>) -> List<PushResultDto> = { defaultPushResult(it) }

  /** Photos the fake server already holds; uploads add to this set. */
  val serverPhotos = mutableSetOf<String>()

  private fun defaultPushResult(records: List<RecordDto>): List<PushResultDto> =
    records.map { record ->
      PushResultDto(
        record.client.id,
        "accepted",
        missingPhotos = record.fotos.map { it.id }.filter { it !in serverPhotos },
      )
    }
  var pullResponse: PullResponseDto = PullResponseDto()
  /** Optional queue of pull responses consumed one per call (for pagination tests). */
  val pullQueue = ArrayDeque<PullResponseDto>()
  var deleteResult: (String) -> Boolean = { true }
  var failWith: Exception? = null

  /** Server time reported by [fetchServerTime]; defaults to the device clock. */
  var serverTime: Long = System.currentTimeMillis() / 1000

  /** Signals that a pull has started; completed via [pullGate] awaiting is optional. */
  var pullStarted: kotlinx.coroutines.CompletableDeferred<Unit>? = null
  var pullGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

  val pushed = mutableListOf<List<RecordDto>>()
  val pushIdempotencyKeys = mutableListOf<String?>()
  val deleted = mutableListOf<String>()
  var pushCalls = 0

  /** When set, pushes larger than this are rejected with HTTP 422. */
  var maxAcceptedBatch: Int? = null

  /** When true, the next push fails with a transient error. */
  var failPushOnce: Boolean = false

  override suspend fun push(
    records: List<RecordDto>,
    timestamp: Long,
    idempotencyKey: String?,
  ): List<PushResultDto> {
    failWith?.let { throw it }
    maxAcceptedBatch?.let { max ->
      if (records.size > max) throw ServerException("batch too large", 422)
    }
    if (failPushOnce) {
      failPushOnce = false
      throw java.io.IOException("transient push failure")
    }
    pushCalls++
    pushed.add(records)
    pushIdempotencyKeys.add(idempotencyKey)
    return pushResult(records)
  }

  override suspend fun pull(timestamp: Long, since: Long?, sinceId: String?, limit: Int?): PullResponseDto {
    failWith?.let { throw it }
    pulledSinces.add(since)
    pulledLimits.add(limit)
    pullStarted?.complete(Unit)
    pullGate?.await()
    return if (pullQueue.isNotEmpty()) pullQueue.removeFirst() else pullResponse
  }

  val pulledSinces = mutableListOf<Long?>()

  /** Page size requested on each pull (null when the caller left it unset). */
  val pulledLimits = mutableListOf<Int?>()

  override suspend fun delete(id: String, timestamp: Long, deletedAt: Long?): Boolean {
    failWith?.let { throw it }
    deleted.add(id)
    return deleteResult(id)
  }

  override suspend fun fetchServerTime(): Long = serverTime

  /**
   * Latest data timestamp returned by [fetchServerInfo]; null to simulate failure.
   * Defaults to null so tests that do not exercise the server-info check keep the
   * previous best-effort behaviour.
   */
  var latestUpdatedAt: Long? = null

  /** Client count returned by [fetchServerInfo]. */
  var clientCount: Int = 0

  /** Stable server identity returned by [fetchServerInfo]; null for old servers. */
  var instanceId: String? = null

  /** Push batch limit returned by [fetchServerInfo]; null for old servers. */
  var maxPushRecords: Int? = null

  override suspend fun fetchServerInfo(timestamp: Long): ServerInfo {
    failWith?.let { throw it }
    return ServerInfo(
      latestUpdatedAt ?: throw java.io.IOException("no latest"),
      clientCount,
      instanceId = instanceId,
      maxPushRecords = maxPushRecords,
    )
  }

  /** Records returned by [fetchClients]. */
  var clientsResponse: List<ClienteOutDto> = emptyList()

  override suspend fun fetchClients(timestamp: Long): List<ClienteOutDto> {
    failWith?.let { throw it }
    return clientsResponse
  }

  /** Entries returned by [fetchClientHistory]; empty by default. */
  var historyResponse: (String) -> List<com.example.elisdb.remote.AuditEntryDto> = { emptyList() }

  override suspend fun fetchClientHistory(id: String, timestamp: Long): List<com.example.elisdb.remote.AuditEntryDto> {
    failWith?.let { throw it }
    return historyResponse(id)
  }

  /** Page fetcher override; defaults to the full [historyResponse]. */
  var historyPageResponse: ((String, Int, Int?) -> HistoryPageDto)? = null

  override suspend fun fetchClientHistoryPage(
    id: String,
    limit: Int,
    afterId: Int?,
    timestamp: Long,
  ): HistoryPageDto {
    failWith?.let { throw it }
    return historyPageResponse?.invoke(id, limit, afterId)
      ?: HistoryPageDto(historyResponse(id), null)
  }

  /** Server-side photo bytes keyed by id, served by [downloadPhoto]. */
  val photos = mutableMapOf<String, ByteArray>()

  /** Number of transient failures [downloadPhoto] throws before succeeding. */
  var photoFailures = 0

  /** How many times [downloadPhoto] was called. */
  var photoDownloadCalls = 0

  /** When set, the next photo upload throws this exception. */
  var failUpload: Exception? = null

  /** Photos uploaded via [uploadPhoto], keyed by id. */
  val uploaded = mutableMapOf<String, ByteArray>()

  override suspend fun downloadPhoto(id: String, timestamp: Long): ByteArray {
    failWith?.let { throw it }
    photoDownloadCalls++
    if (photoFailures > 0) {
      photoFailures--
      throw java.io.IOException("transient photo failure")
    }
    return photos[id] ?: throw java.io.IOException("no photo $id")
  }

  override suspend fun uploadPhoto(id: String, bytes: ByteArray, timestamp: Long) {
    failWith?.let { throw it }
    failUpload?.let { throw it }
    uploaded[id] = bytes
    serverPhotos += id
  }
}

/** Helper builders. */
fun pullResponseOf(vararg items: ClienteComFotosDto, tombstones: List<TombstoneDto> = emptyList()): PullResponseDto =
  PullResponseDto(records = items.toList(), tombstones = tombstones)

fun serverItem(
  id: String = "s1",
  nome: String = "Server",
  updatedAt: Long = 100,
  createdAt: Long? = null,
  latitude: Double? = 0.0,
  longitude: Double? = 0.0,
  fotos: List<FotoOutDto> = emptyList(),
  photosComplete: Boolean = true,
  deliveryTs: Long? = null,
): ClienteComFotosDto =
  ClienteComFotosDto(
    client =
      ClienteOutDto(
        id = id,
        nome = nome,
        latitude = latitude,
        longitude = longitude,
        updatedAt = updatedAt,
        createdAt = createdAt,
        deliveryTs = deliveryTs,
      ),
    fotos = fotos,
    photosComplete = photosComplete,
  )
/** Base64-decodes [dataB64] into bytes. */
fun decoded(dataB64: String): ByteArray =
  byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) +
    java.util.Base64.getDecoder().decode(dataB64)

/** SHA-256 hex of the decoded [dataB64], matching the server's photo hash. */
fun sha256Of(dataB64: String): String = PhotoHashes.sha256(decoded(dataB64))

/** In-memory fake [LastSyncStore]. */
class FakeLastSyncStore(var value: Long? = null) : LastSyncStore {
  override fun getLastSuccessAt(): Long? = value

  override fun setLastSuccessAt(epochSeconds: Long) {
    value = epochSeconds
  }

  override fun clear() {
    value = null
  }
}

/** In-memory [ServerIdentityStore]. */
class FakeServerIdentityStore(var value: String? = null) : ServerIdentityStore {
  override fun get(): String? = value
  override fun set(instanceId: String) { value = instanceId }
  override fun clear() { value = null }
}
