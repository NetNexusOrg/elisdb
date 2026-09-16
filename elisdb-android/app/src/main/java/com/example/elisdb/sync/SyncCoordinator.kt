package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteDao
import com.example.elisdb.data.local.Foto
import com.example.elisdb.data.local.FotoDao
import com.example.elisdb.remote.AuthCause
import com.example.elisdb.remote.AuthException
import com.example.elisdb.remote.ClienteComFotosDto
import com.example.elisdb.remote.ConfigException
import com.example.elisdb.remote.TombstoneDto
import java.util.Locale
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates offline-first sync. The server owns conflicts; this layer only
 * pushes pending data, pulls the authoritative state, and applies confirms.
 */
class SyncCoordinator(
  private val api: SyncApi,
  private val clienteDao: ClienteDao,
  private val fotoDao: FotoDao,
  private val photoStore: PhotoStore,
  private val transaction: suspend (suspend () -> Unit) -> Unit = { block -> block() },
  private val pullWatermark: PullWatermark? = null,
  /**
   * Incremental pulls (R7): send `since` so the server only returns changed
   * records. Safe now that `sync-incremental-photos` (R3) skips unchanged
   * photos by content instead of rewriting every photo on each pull.
   */
  private val incrementalPullEnabled: Boolean = true,
  private val photoSweeper: PhotoSweeper? = null,
  private val clockOffsetStore: ClockOffsetStore? = null,
  private val photoPushState: PhotoPushState? = null,
  private val onWarning: (String) -> Unit = {},
) {
  /** Device clock (epoch seconds) shifted by the last-known server offset, when available. */
  private fun normalizedEpochSeconds(): Long =
    System.currentTimeMillis() / 1000 + (clockOffsetStore?.getOffsetSeconds() ?: 0L)

  suspend fun sync(): SyncOutcome =
    try {
      refreshClockOffset()
      val (staleNames, mergedNome) = pushEdits()
      pushDeletions()
      pull()
      when {
        mergedNome != null -> SyncOutcome.DuplicateMerged(mergedNome)
        staleNames.isNotEmpty() ->
          SyncOutcome.SuccessWithStatuses(accepted = 0, stale = staleNames)
        else -> SyncOutcome.Success
      }
    } catch (e: ConfigException) {
      SyncOutcome.ConfigFault
    } catch (e: AuthException) {
      if (e.kind == AuthCause.STALE_CLOCK) SyncOutcome.ClockFailure
      else SyncOutcome.AuthFailure
    } catch (e: Exception) {
      SyncOutcome.TransientError
    }

  /** Refreshes the persisted clock offset from the server time, best-effort. */
  private suspend fun refreshClockOffset() {
    val store = clockOffsetStore ?: return
    try {
      val serverTime = api.fetchServerTime()
      val offset = serverTime - System.currentTimeMillis() / 1000
      store.setOffsetSeconds(offset)
    } catch (_: Exception) {
      // Best-effort: stale offset is better than failing the whole sync.
    }
  }

  private suspend fun runInTransaction(block: suspend () -> Unit) {
    transaction(block)
  }

  /** Returns (stale names, merged name) after pushing pending edits. */
  private suspend fun pushEdits(): Pair<List<String>, String?> {
    val pending = clienteDao.getPending().filter { !it.deletePending }
    if (pending.isEmpty()) return Pair(emptyList(), null)

    val records = mutableListOf<com.example.elisdb.remote.RecordDto>()
    val photosToMark = mutableListOf<Foto>()
    val hashesByCliente = mutableMapOf<String, Map<String, String>>()
    for (cliente in pending) {
      val fotos = fotoDao.getByCliente(cliente.id).filter { !it.deletePending }
      val available = mutableListOf<Foto>()
      val hashesById = mutableMapOf<String, String>()
      val knownHashes = photoPushState?.getHashes(cliente.id).orEmpty()
      for (foto in fotos) {
        val bytes = photoStore.readBytes(foto.uriLocal)
        if (bytes == null) {
          photosToMark.add(foto)
          onWarning("Foto ${foto.id} (cliente ${cliente.id}) sem ficheiro; marcada para eliminação")
        } else {
          val hash = PhotoHashes.sha256(bytes)
          if (knownHashes[foto.id] != hash) {
            api.uploadPhoto(foto.id, bytes, timestamp = normalizedEpochSeconds())
          }
          hashesById[foto.id] = hash
          available.add(foto)
        }
      }
      hashesByCliente[cliente.id] = hashesById
      records.add(SyncMapper.toRecord(cliente, available))
    }
    if (photosToMark.isNotEmpty()) {
      runInTransaction { photosToMark.forEach { fotoDao.update(it.copy(deletePending = true)) } }
    }

    val results = api.push(records, timestamp = normalizedEpochSeconds())
    val byId = results.associateBy { it.id }

    var mergedNome: String? = null
    val staleNames = mutableListOf<String>()
    val filesToDelete = mutableSetOf<String>()
    runInTransaction {
      pending.forEach { cliente ->
        val result = byId[cliente.id]
        val status = result?.status
        if (status == "duplicate" && result.existingId != null) {
          // The server already has a record with the same normalized name. Adopt
          // the canonical id but keep the user's local edits pending so they
          // re-push and the server stores the merged values (LWW by updated_at).
          filesToDelete += mergeDuplicate(cliente, result)
          mergedNome = cliente.nome
        } else if (status == "stale") {
          // The server has a newer version; it wins. Clear pending so the pull
          // reconciles, but report the lost edit to the user.
          staleNames += cliente.nome
          clienteDao.update(cliente.copy(pending = false))
        } else if (status == "error") {
          // The server rejected this record (e.g. blank name, future timestamp).
          // Keep it pending so the user's data is not lost, and surface a warning.
          onWarning("Servidor rejeitou o registo ${cliente.id}; mantido pendente")
        } else if (status != null) {
          // Any other server response (accepted) is an ack: the server owns
          // conflict resolution, so clear pending and let pull reconcile.
          clienteDao.update(cliente.copy(pending = false))
          recordPushedHashes(cliente.id, hashesByCliente[cliente.id].orEmpty())
        }
      }
    }
    // Superseded photo files are removed only after the push transaction has
    // committed, so a later failure in the loop cannot leave Foto rows pointing
    // at deleted files.
    filesToDelete.forEach { photoStore.delete(it) }
    return staleNames to mergedNome
  }

  /** Records the current photo content hashes after a successful push ack. */
  private fun recordPushedHashes(clienteId: String, hashesById: Map<String, String>) {
    photoPushState?.setHashes(clienteId, hashesById)
  }

  /**
   * Re-attributes the local record onto the canonical id and merges photos so no
   * photo is lost: the canonical record's photos (from the duplicate response)
   * are materialized, the local record's photos are re-attributed, and the old
   * id is deleted. Uses `upsert` (never REPLACE) so the fotos FK CASCADE is not
   * triggered when the canonical client already exists locally.
   */
  private suspend fun mergeDuplicate(cliente: Cliente, result: com.example.elisdb.remote.PushResultDto): Set<String> {
    val existingId = result.existingId ?: return emptySet()
    // Canonical photos may not be stored locally yet (fresh install, or the
    // canonical record only lives on the server), so materialize them from the
    // duplicate response before touching any rows.
    val materializer = PhotoMaterializer(photoStore)
    val existingCanonicalFotos = fotoDao.getByCliente(existingId)
    val canonicalUpserts = mutableListOf<Foto>()
    val canonicalKeptIds = mutableSetOf<String>()
    val filesToDeleteAfterCommit = mutableSetOf<String>()
    for (fotoDto in result.fotos) {
      val existing = existingCanonicalFotos.firstOrNull { it.id == fotoDto.id }
      when (val m = materializer.materialize(existing, fotoDto, download = { api.downloadPhoto(it, timestamp = normalizedEpochSeconds()) })) {
        is MaterializeResult.Keep -> canonicalKeptIds += fotoDto.id
        is MaterializeResult.Repointed -> {
          canonicalUpserts +=
            Foto(
              id = fotoDto.id,
              clienteId = existingId,
              uriLocal = m.uriLocal,
              mime = fotoDto.mime,
              ordem = fotoDto.ordem,
            )
          if (existing != null) {
            canonicalKeptIds += existing.id
            filesToDeleteAfterCommit += existing.uriLocal
          }
        }
      }
    }
    // Local photos of the canonical record the server no longer carries are
    // removed together with their rows and files.
    val canonicalRemoved = existingCanonicalFotos.filter { it.id !in canonicalKeptIds }
    val removedRows = canonicalRemoved.map { it.id }
    canonicalRemoved.forEach { filesToDeleteAfterCommit += it.uriLocal }

    val localFotos = fotoDao.getByCliente(cliente.id).filter { !it.deletePending }
    val now = normalizedEpochSeconds()
    runInTransaction {
      // upsert (not insert/REPLACE): preserves any canonical row already stored,
      // so its existing photo rows survive until the union is committed below.
      clienteDao.upsert(
        cliente.copy(
          id = existingId,
          updatedAt = maxOf(cliente.updatedAt, now),
          pending = true,
          deletePending = false,
        )
      )
      canonicalUpserts.forEach { fotoDao.insert(it) }
      localFotos.forEach { fotoDao.insert(it.copy(clienteId = existingId)) }
      if (removedRows.isNotEmpty()) fotoDao.deleteByIds(removedRows)
      clienteDao.delete(cliente)
    }
    // Old files are returned (not deleted here): the enclosing push transaction
    // is still open, so deleting now could leave Foto rows pointing at missing
    // files if that transaction rolls back. The caller deletes them after commit.
    return filesToDeleteAfterCommit
  }

  private suspend fun pushDeletions() {
    val deletions = clienteDao.getPendingDeletions()
    if (deletions.isEmpty()) return

    deletions.forEach { cliente ->
      val confirmed =
        try {
          api.delete(cliente.id, timestamp = normalizedEpochSeconds(), deletedAt = normalizedEpochSeconds())
        } catch (e: AuthException) {
          throw e
        } catch (e: ConfigException) {
          throw e
        } catch (e: Exception) {
          onWarning("Falha ao eliminar cliente ${cliente.id} no servidor: ${e.message ?: "erro"}")
          false
        }
      if (confirmed) {
        finalizeDeletion(cliente.id).forEach { photoStore.delete(it) }
      } else {
        onWarning("Servidor não confirmou a eliminação do cliente ${cliente.id}")
      }
    }
  }

  /**
   * Removes a client's rows and returns the photo files to delete. Files are
   * deleted by the caller only after the enclosing transaction has committed,
   * so a rollback can never leave rows pointing at deleted files.
   */
  private suspend fun finalizeDeletion(clienteId: String): Set<String> {
    val filesToDelete = mutableSetOf<String>()
    runInTransaction {
      val fotos = fotoDao.getByCliente(clienteId)
      fotos.forEach { filesToDelete += it.uriLocal }
      if (fotos.isNotEmpty()) fotoDao.deleteByIds(fotos.map { it.id })
      clienteDao.getById(clienteId)?.let { clienteDao.delete(it) }
    }
    // Drop the persisted push-hash entry so a deleted client's tracking does not
    // linger forever in SharedPreferences.
    photoPushState?.clear(clienteId)
    return filesToDelete
  }

  private suspend fun pull() {
    PhotoStoreGuard.mutex.withLock {
      val watermark = if (incrementalPullEnabled) pullWatermark?.get() else null
      val isFullPull = watermark == null
      // Follow the server's pagination cursor to the last page before applying,
      // so a paginated pull never drops local records based on a partial page.
      var since = watermark
      var sinceId: String? = null
      val allServerClients = mutableListOf<ClienteComFotosDto>()
      val allTombstones = mutableListOf<TombstoneDto>()
      do {
        val response = api.pull(since = since, sinceId = sinceId, timestamp = normalizedEpochSeconds())
        allServerClients += response.records
        allTombstones += response.tombstones
        since = response.nextSince
        sinceId = response.nextSinceId
      } while (since != null)

      // Apply server state for records with no pending local edits. Each item
      // materializes its photo files before its own Room transaction, so a file
      // is always committed before the row that references it (and old files are
      // only removed after the row commit succeeds).
      allServerClients.forEach { serverItem ->
        val local = clienteDao.getById(serverItem.client.id)
        if (local == null || (!local.pending && !local.deletePending)) {
          applyServerItem(serverItem)
        }
      }

      val filesToDelete = mutableSetOf<String>()
      runInTransaction {
        // Tombstoned records are deleted locally unless they have pending edits,
        // so a delete made on another device propagates even on delta pulls.
        allTombstones.forEach { tombstone ->
          val local = clienteDao.getById(tombstone.id)
          if (local != null && !local.pending && !local.deletePending) {
            filesToDelete += finalizeDeletion(tombstone.id)
          }
        }

        // Drop local records the server no longer has, unless they are pending.
        // Only safe on a full pull (after the complete page set is received):
        // deletes are otherwise removed via the delete endpoint, and a delta
        // pull simply omits older records.
        if (isFullPull) {
          val serverIds = allServerClients.mapTo(mutableSetOf()) { it.client.id }
          clienteDao.getAll().forEach { local ->
            if (local.id !in serverIds && !local.pending && !local.deletePending) {
              filesToDelete += finalizeDeletion(local.id)
            }
          }
        }

        // Advance the watermark to the largest updated_at / deleted_at seen on
        // this pull so later pulls are incremental (and do not re-apply or
        // re-discover already-seen tombstones).
        val maxUpdatedAt =
          maxOf(
            allServerClients.maxOfOrNull { it.client.updatedAt } ?: 0L,
            allTombstones.maxOfOrNull { it.deletedAt } ?: 0L,
          )
        if (maxUpdatedAt > 0 && pullWatermark != null) {
          val previous = pullWatermark.get()
          if (previous == null || maxUpdatedAt > previous) {
            pullWatermark.set(maxUpdatedAt)
          }
        }
      }
      // Photo files removed by the deletions above are deleted only after the
      // transaction has committed, so a rollback cannot leave rows pointing at
      // deleted files.
      filesToDelete.forEach { photoStore.delete(it) }

      // Reclaim files left over after a successful pull, inside the same guard
      // that serializes materialization vs. any app-start sweep (R8).
      photoSweeper?.sweep()
    }
  }

  /**
   * Applies one authoritative server item. Photo files for changed photos are
   * written first (two-phase), then rows are committed in a per-item
   * transaction; replaced/removed old files are deleted only after the commit.
   */
  private suspend fun applyServerItem(serverItem: ClienteComFotosDto) {
    val server = serverItem.client
    val existingLocal = clienteDao.getById(server.id)
    val createdAt =
      server.createdAt
        ?: existingLocal?.dataCriacao
        ?: server.updatedAt
    val cliente =
      Cliente(
        id = server.id,
        nome = server.nome.uppercase(Locale.ROOT),
        contrato = server.contrato,
        latitude = server.latitude,
        longitude = server.longitude,
        notas = server.notas,
        dataCriacao = createdAt,
        updatedAt = server.updatedAt,
        createdAt = createdAt,
        pending = false,
        deletePending = false,
      )

    val materializer = PhotoMaterializer(photoStore)
    val existingFotos = fotoDao.getByCliente(cliente.id)
    val upserts = mutableListOf<Foto>()
    val keptIds = mutableSetOf<String>()
    val filesToDeleteAfterCommit = mutableSetOf<String>()

    // Compare + materialize files outside the transaction.
    for (fotoDto in serverItem.fotos) {
      val existing = existingFotos.firstOrNull { it.id == fotoDto.id }
      when (val result = materializer.materialize(existing, fotoDto, download = { api.downloadPhoto(it, timestamp = normalizedEpochSeconds()) })) {
        is MaterializeResult.Keep ->
          if (existing != null) keptIds += existing.id
        is MaterializeResult.Repointed -> {
          upserts +=
            Foto(
              id = fotoDto.id,
              clienteId = cliente.id,
              uriLocal = result.uriLocal,
              mime = fotoDto.mime,
              ordem = fotoDto.ordem,
            )
          if (existing != null) {
            keptIds += existing.id
            filesToDeleteAfterCommit += existing.uriLocal
          }
        }
      }
    }

    // Local photos the server no longer carries are removed with their rows.
    val removedRows = existingFotos.filter { it.id !in keptIds }
    removedRows.forEach { filesToDeleteAfterCommit += it.uriLocal }

    runInTransaction {
      // Use upsert (not REPLACE): REPLACE deletes the existing client row and
      // the fotos FK CASCADE would wipe every photo of a client that already
      // exists locally, even ones the server still carries.
      clienteDao.upsert(cliente)
      upserts.forEach { fotoDao.insert(it) }
      if (removedRows.isNotEmpty()) fotoDao.deleteByIds(removedRows.map { it.id })
    }

    // Old files are safe to delete only after the new rows are committed.
    filesToDeleteAfterCommit.forEach { photoStore.delete(it) }
  }
}