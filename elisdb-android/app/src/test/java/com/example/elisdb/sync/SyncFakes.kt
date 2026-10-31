package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteDao
import com.example.elisdb.data.local.Documento
import com.example.elisdb.data.local.DocumentoDao
import com.example.elisdb.data.local.Foto
import com.example.elisdb.data.local.FotoDao
import com.example.elisdb.remote.ClienteComFotosDto
import com.example.elisdb.remote.ClienteOutDto
import com.example.elisdb.remote.DocumentoOutDto
import com.example.elisdb.remote.FotoOutDto
import com.example.elisdb.remote.PullResponseDto
import com.example.elisdb.remote.PushResultDto
import com.example.elisdb.remote.RecordDto
import com.example.elisdb.remote.TombstoneDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** In-memory fake [ClienteDao]. */
class FakeClienteDao(initial: List<Cliente> = emptyList()) : ClienteDao {  val data = initial.toMutableList()

  override fun observeAll(): Flow<List<Cliente>> = flowOf(data.filter { !it.deletePending })
  override fun observeAllIncludingDeleted(): Flow<List<Cliente>> = flowOf(data.toList())
  override fun observeCount(): Flow<Int> = flowOf(data.count { !it.deletePending })

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

/** In-memory [DocumentoDao] for device-map tests. */
class FakeDocumentoDao(initial: List<Documento> = emptyList()) : DocumentoDao {
  val data = initial.toMutableList()

  override suspend fun getByCliente(clienteId: String): List<Documento> =
    data.filter { it.clienteId == clienteId && !it.deletePending }.sortedWith(compareBy({ it.ordem }, { it.id }))

  override suspend fun getAllByCliente(clienteId: String): List<Documento> =
    data.filter { it.clienteId == clienteId }.sortedWith(compareBy({ it.ordem }, { it.id }))

  override suspend fun insert(documento: Documento) {
    data.removeAll { it.id == documento.id }
    data.add(documento)
  }

  override suspend fun deleteByIds(ids: List<String>) {
    data.removeAll { it.id in ids }
  }

  override suspend fun update(documento: Documento) {
    val i = data.indexOfFirst { it.id == documento.id }
    if (i >= 0) data[i] = documento else data.add(documento)
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
    content[uri] = java.util.Base64.getDecoder().decode(dataB64)
  }

  /** Base64 of the stored bytes at [uri], for assertions. */
  fun contentB64(uri: String): String =
    java.util.Base64.getEncoder().encodeToString(content[uri]!!)

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
  override suspend fun get(): Long? = value
  override suspend fun set(value: Long) {
    this.value = value
  }

  override suspend fun clear() {
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
  var pushResult: (List<RecordDto>) -> List<PushResultDto> = { it.map { r -> PushResultDto(r.client.id, "accepted") } }
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
  val deleted = mutableListOf<String>()
  var pushCalls = 0

  override suspend fun push(records: List<RecordDto>, timestamp: Long): List<PushResultDto> {
    failWith?.let { throw it }
    pushCalls++
    pushed.add(records)
    return pushResult(records)
  }

  override suspend fun pull(timestamp: Long, since: Long?, sinceId: String?): PullResponseDto {
    failWith?.let { throw it }
    pulledSinces.add(since)
    pullStarted?.complete(Unit)
    pullGate?.await()
    return if (pullQueue.isNotEmpty()) pullQueue.removeFirst() else pullResponse
  }

  val pulledSinces = mutableListOf<Long?>()

  override suspend fun delete(id: String, timestamp: Long, deletedAt: Long?): Boolean {
    failWith?.let { throw it }
    deleted.add(id)
    return deleteResult(id)
  }

  override suspend fun fetchServerTime(): Long = serverTime

  /** Latest data timestamp returned by [fetchServerInfo]; null to simulate failure. */
  var latestUpdatedAt: Long? = 0L

  /** Client count returned by [fetchServerInfo]. */
  var clientCount: Int = 0

  override suspend fun fetchServerInfo(timestamp: Long): ServerInfo {
    failWith?.let { throw it }
    return ServerInfo(latestUpdatedAt ?: throw java.io.IOException("no latest"), clientCount)
  }

  /** Entries returned by [fetchClientHistory]; empty by default. */
  var historyResponse: (String) -> List<com.example.elisdb.remote.AuditEntryDto> = { emptyList() }

  override suspend fun fetchClientHistory(id: String, timestamp: Long): List<com.example.elisdb.remote.AuditEntryDto> {
    failWith?.let { throw it }
    return historyResponse(id)
  }

  /** Server-side photo bytes keyed by id, served by [downloadPhoto]. */
  val photos = mutableMapOf<String, ByteArray>()

  /** Number of transient failures [downloadPhoto] throws before succeeding. */
  var photoFailures = 0

  /** How many times [downloadPhoto] was called. */
  var photoDownloadCalls = 0

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
    uploaded[id] = bytes
  }

  /** Server-side device-map document bytes keyed by id. */
  val documents = mutableMapOf<String, ByteArray>()

  /** How many times [downloadDocument] was called. */
  var documentDownloadCalls = 0

  /** Documents uploaded via [uploadDocument], keyed by id. */
  val uploadedDocuments = mutableMapOf<String, ByteArray>()

  /** How many times [uploadDocument] was called. */
  var documentUploadCalls = 0

  override suspend fun downloadDocument(id: String, timestamp: Long): ByteArray {
    failWith?.let { throw it }
    documentDownloadCalls++
    if (photoFailures > 0) {
      photoFailures--
      throw java.io.IOException("transient document failure")
    }
    return documents[id] ?: throw java.io.IOException("no document $id")
  }

  override suspend fun uploadDocument(id: String, bytes: ByteArray, timestamp: Long) {
    failWith?.let { throw it }
    documentUploadCalls++
    uploadedDocuments[id] = bytes
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
  documentos: List<DocumentoOutDto> = emptyList(),
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
    documentos = documentos,
  )
/** Base64-decodes [dataB64] into bytes. */
fun decoded(dataB64: String): ByteArray = java.util.Base64.getDecoder().decode(dataB64)

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
