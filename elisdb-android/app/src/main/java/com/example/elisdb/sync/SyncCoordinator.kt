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
  /**
   * Receives the unexpected exception that aborts a run. The UI only shows a
   * generic network message, so this keeps the real cause diagnosable.
   */
  private val onError: (Throwable) -> Unit = {},
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
    } catch (e: kotlinx.coroutines.CancellationException) {
      throw e
    } catch (e: Exception) {
      onError(e)
      SyncOutcome.TransientError
    }

  /**
   * Downloads a photo, retrying once on a transient failure. Photo GETs are
   * idempotent, so one retry is cheap and keeps a momentary network hiccup from
   * aborting the whole pull (which discards its progress and forces a full retry).
   */
  private suspend fun downloadPhoto(id: String): ByteArray =
    try {
      api.downloadPhoto(id, normalizedEpochSeconds())
    } catch (e: kotlinx.coroutines.CancellationException) {
      throw e
    } catch (e: AuthException) {
      throw e
    } catch (e: ConfigException) {
      throw e
    } catch (e: Exception) {
      onWarning("Falha ao descarregar a foto $id; a tentar novamente")
      kotlinx.coroutines.delay(PHOTO_RETRY_DELAY_MS)
      api.downloadPhoto(id, normalizedEpochSeconds())
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
    val hashesByCliente = mutableMapOf<String, Map<String, String>>()
    for (cliente in pending) {
      val fotos = fotoDao.getByCliente(cliente.id).filter { !it.deletePending }
      val available = mutableListOf<Foto>()
      val hashesById = mutableMapOf<String, String>()
      val bytesById = mutableListOf<Pair<Foto, ByteArray>>()
      var missing: Foto? = null
      for (foto in fotos) {
        val bytes = photoStore.readBytes(foto.uriLocal)
        if (bytes == null) {
          missing = foto
          break
        }
        bytesById.add(foto to bytes)
      }
      if (missing != null) {
        // Never push a record that silently omits a photo: the server would
        // treat the omission as a removal and every device would delete its
        // copy. Keep the client pending and retry on a later sync.
        onWarning(
          "Foto ${missing.id} (cliente ${cliente.id}) sem ficheiro; sincronização do cliente adiada"
        )
        continue
      }
      val knownHashes = photoPushState?.getHashes(cliente.id).orEmpty()
      for ((foto, bytes) in bytesById) {
        val hash = PhotoHashes.sha256(bytes)
        if (knownHashes[foto.id] != hash) {
          api.uploadPhoto(foto.id, bytes, timestamp = normalizedEpochSeconds())
        }
        hashesById[foto.id] = hash
        available.add(foto)
      }
      hashesByCliente[cliente.id] = hashesById
      records.add(SyncMapper.toRecord(cliente, available))
    }
    if (records.isEmpty()) return emptyList<String>() to null

    val results = api.push(records, timestamp = normalizedEpochSeconds())
    val byId = results.associateBy { it.id }

    var mergedNome: String? = null
    val staleNames = mutableListOf<String>()
    val filesToDelete = mutableSetOf<String>()
    // One clear per sync is enough: the pull that follows re-fetches the server
    // state, so further stale acks in the same run do not need another reset.
    var clearWatermarkAfterPush = false
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
          // The server has a newer version, so the edit that was sent loses.
          // Clear pending only while the row still holds exactly that edit: a
          // save made while the push was in flight must stay pending and be sent
          // on the next sync instead of being silently dropped.
          if (clienteDao.markSynced(cliente.id, cliente.revision) == 1) {
            staleNames += cliente.nome
            // The local edit lost to a newer server version. Forget the pull
            // watermark so the pull below re-fetches the server version instead
            // of leaving the two sides permanently divergent.
            clearWatermarkAfterPush = true
          } else {
            onWarning(
              "O cliente ${cliente.nome} mudou durante a sincronização; a alteração será reenviada"
            )
          }
        } else if (status == "error") {
          // The server rejected this record (e.g. blank name, future timestamp).
          // Keep it pending so the user's data is not lost, and surface a warning.
          onWarning("Servidor rejeitou o registo ${cliente.id}; mantido pendente")
        } else if (status != null) {
          // Any other server response (accepted) is an ack: the server owns
          // conflict resolution, so clear pending and let pull reconcile. Only
          // clear it while the row is still the version that was sent; an edit
          // made during the push stays pending for the next sync.
          val missing = result.missingPhotos.toSet()
          if (missing.isNotEmpty()) {
            // The server kept a placeholder row but has no bytes. Keep the
            // client pending so the next sync retries the upload, and do not
            // record the missing photos as already uploaded.
            recordPushedHashes(
              cliente.id,
              hashesByCliente[cliente.id].orEmpty().filterKeys { it !in missing },
            )
            onWarning(
              "O servidor não tem as fotos ${missing.joinToString()} do cliente ${cliente.id}; serão reenviadas"
            )
          } else if (clienteDao.markSynced(cliente.id, cliente.revision) == 1) {
            recordPushedHashes(cliente.id, hashesByCliente[cliente.id].orEmpty())
          }
        }
      }
    }
    // Superseded photo files are removed only after the push transaction has
    // committed, so a later failure in the loop cannot leave Foto rows pointing
    // at deleted files.
    filesToDelete.forEach { photoStore.delete(it) }
    if (clearWatermarkAfterPush) pullWatermark?.clear()
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
    val filesToDeleteAfterCommit = mutableSetOf<String>()
    for (fotoDto in result.fotos) {
      val existing = existingCanonicalFotos.firstOrNull { it.id == fotoDto.id }
      if (!fotoDto.available) {
        // The server still knows the photo but has lost the bytes. Keep any
        // local copy instead of interpreting the response as a removal.
        continue
      }
      when (val m = materializer.materialize(existing, fotoDto, download = { downloadPhoto(it) })) {
        is MaterializeResult.Keep -> Unit
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
            filesToDeleteAfterCommit += existing.uriLocal
          }
        }
      }
    }
    // Do not delete canonical photos that the duplicate response does not list:
    // a partial/incomplete response must never destroy a local copy. Explicit
    // removals arrive through the normal pull path with a complete photo list.

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
      var watermark = if (incrementalPullEnabled) pullWatermark?.get() else null
      // A server restored from an older backup can report a latest timestamp
      // below the local cursor; a delta pull would then never return the
      // restored records. Detect it and fall back to a full pull. Best effort:
      // if the status call fails, keep the current incremental behaviour.
      if (watermark != null) {
        val latest =
          try {
            api.fetchServerInfo(timestamp = normalizedEpochSeconds()).latestUpdatedAt
          } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
          } catch (_: Exception) {
            null
          }
        if (latest != null && latest < watermark) {
          pullWatermark?.clear()
          watermark = null
          onWarning(
            "O servidor parece ter sido restaurado de uma cópia mais antiga; a sincronizar tudo de novo"
          )
        }
      }
      val isFullPull = watermark == null
      // Follow the server's pagination cursor to the last page before applying,
      // so a paginated pull never drops local records based on a partial page.
      var since = watermark
      var sinceId: String? = null
      val allServerClients = mutableListOf<ClienteComFotosDto>()
      val allTombstones = mutableListOf<TombstoneDto>()
      do {
        val response =
          api.pull(
            since = since,
            sinceId = sinceId,
            timestamp = normalizedEpochSeconds(),
            limit = PULL_PAGE_SIZE,
          )
        allServerClients += response.records
        allTombstones += response.tombstones
        val nextSince = response.nextSince
        val nextSinceId = response.nextSinceId
        // A well-behaved server advances the cursor past the last record on every
        // page; guard against one that keeps returning the same cursor forever.
        if (nextSince != null && nextSince == since && nextSinceId == sinceId) {
          onWarning("Cursor de paginação não avançou; a interromper o pull")
          break
        }
        since = nextSince
        sinceId = nextSinceId
      } while (since != null)

      // Apply server state for records with no pending local edits. Each item
      // materializes its photo files before its own Room transaction, so a file
      // is always committed before the row that references it (and old files are
      // only removed after the row commit succeeds).
      // Delivery timestamps of records that were NOT applied: the watermark may
      // not move past them, or the next delta pull would never return them.
      val skippedDelivery = mutableListOf<Long>()
      allServerClients.forEach { serverItem ->
        when (applyServerItem(serverItem)) {
          ApplyOutcome.APPLIED -> Unit
          ApplyOutcome.SKIPPED_HOLDS_WATERMARK -> skippedDelivery += deliveryOf(serverItem)
          // The pending record is too old to block the rest of the sync: it stays
          // pending and is retried, but the watermark may move past the version
          // the server holds (see `holdsWatermark`).
          ApplyOutcome.SKIPPED_RELEASES_WATERMARK -> Unit
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
          } else if (local != null && holdsWatermark(local)) {
            // Skipped: the pending local edit still has to be pushed, and the
            // deletion has to come back on the next pull.
            skippedDelivery += deliveryOf(tombstone)
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

        // Advance the watermark to the largest delivery timestamp seen on this
        // pull (the server's cursor, not the device clocks), holding it below the
        // oldest record that was skipped because of a pending local edit.
        var maxDelivery =
          maxOf(
            allServerClients.maxOfOrNull { deliveryOf(it) } ?: 0L,
            allTombstones.maxOfOrNull { deliveryOf(it) } ?: 0L,
          )
        val oldestSkipped = skippedDelivery.minOrNull()
        if (oldestSkipped != null && maxDelivery >= oldestSkipped) {
          maxDelivery = oldestSkipped - 1
        }
        if (maxDelivery > 0 && pullWatermark != null) {
          val previous = pullWatermark.get()
          if (previous == null || maxDelivery > previous) {
            pullWatermark.set(maxDelivery)
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
   * Applies one authoritative server item and reports whether it was applied.
   * Photo files for changed photos are written first (two-phase), then rows are
   * committed in a per-item transaction; replaced/removed old files are deleted
   * only after the commit.
   *
   * Returns [ApplyOutcome.SKIPPED_HOLDS_WATERMARK] when the record has pending
   * local work that is recent enough to block the pull cursor, so the server
   * version is left for a later pull and must not be skipped by the watermark.
   * A pending record older than [WATERMARK_HOLD_SECONDS] is reported as
   * [ApplyOutcome.SKIPPED_RELEASES_WATERMARK]: it still has to be pushed, but it
   * is treated as broken and no longer holds the rest of the synchronization.
   */
  private suspend fun applyServerItem(serverItem: ClienteComFotosDto): ApplyOutcome {
    val server = serverItem.client
    val existingLocal = clienteDao.getById(server.id)
    if (existingLocal != null && (existingLocal.pending || existingLocal.deletePending)) {
      return skipOutcome(existingLocal)
    }
    val createdAt =
      server.createdAt
        ?: existingLocal?.dataCriacao
        ?: server.updatedAt
    val cliente =
      Cliente(
        id = server.id,
        nome = server.nome.uppercase(Locale.ROOT),
        contrato = server.contrato,
        tag = server.tag,
        latitude = server.latitude,
        longitude = server.longitude,
        notas = server.notas,
        ativo = server.ativo,
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
      if (!fotoDto.available) {
        // The server still knows the photo but has lost the bytes. Preserve the
        // local copy; do not download or treat it as removed.
        if (existing != null) keptIds += existing.id
        continue
      }
      when (val result = materializer.materialize(existing, fotoDto, download = { downloadPhoto(it) })) {
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

    // Only an explicitly complete server photo list can justify deleting local
    // photos the response does not mention. An incomplete list (older server or
    // partial response) must never be interpreted as a removal.
    val absentLocally = existingFotos.filter { it.id !in keptIds }
    val removedRows =
      if (serverItem.photosComplete) {
        absentLocally
      } else {
        if (absentLocally.isNotEmpty()) {
          onWarning(
            "Lista de fotos incompleta para ${cliente.nome}; fotos locais preservadas"
          )
        }
        emptyList()
      }
    removedRows.forEach { filesToDeleteAfterCommit += it.uriLocal }

    var applied = true
    var holdsCursor = false
    runInTransaction {
      // Re-read inside the transaction: a save during the photo downloads above
      // must win over the server version, not be overwritten by it.
      val current = clienteDao.getById(cliente.id)
      if (current != null && (current.pending || current.deletePending)) {
        applied = false
        holdsCursor = holdsWatermark(current)
      } else {
        // Use upsert (not REPLACE): REPLACE deletes the existing client row and
        // the fotos FK CASCADE would wipe every photo of a client that already
        // exists locally, even ones the server still carries.
        clienteDao.upsert(cliente)
        upserts.forEach { fotoDao.insert(it) }
        if (removedRows.isNotEmpty()) fotoDao.deleteByIds(removedRows.map { it.id })
      }
    }

    // Old files are safe to delete only after the new rows are committed, and
    // only when they were replaced: on a skipped item the current rows still
    // point at them. Files materialized for a skipped item are left to the
    // orphan sweep.
    if (applied) filesToDeleteAfterCommit.forEach { photoStore.delete(it) }
    return when {
      applied -> ApplyOutcome.APPLIED
      holdsCursor -> ApplyOutcome.SKIPPED_HOLDS_WATERMARK
      else -> ApplyOutcome.SKIPPED_RELEASES_WATERMARK
    }
  }

  /**
   * Whether a pending record is recent enough to keep holding the pull
   * watermark. A record the server keeps rejecting (blank name, persistent
   * error) used to block the cursor forever, so every pull re-downloaded
   * everything after it. After [WATERMARK_HOLD_SECONDS] the record stays
   * pending and is retried, but it no longer blocks the rest of the sync.
   */
  private fun holdsWatermark(record: Cliente): Boolean =
    normalizedEpochSeconds() - record.updatedAt <= WATERMARK_HOLD_SECONDS

  private fun skipOutcome(record: Cliente): ApplyOutcome =
    if (holdsWatermark(record)) ApplyOutcome.SKIPPED_HOLDS_WATERMARK
    else ApplyOutcome.SKIPPED_RELEASES_WATERMARK

  /** How a server item affected the local state, including the effect on the pull cursor. */
  private enum class ApplyOutcome { APPLIED, SKIPPED_HOLDS_WATERMARK, SKIPPED_RELEASES_WATERMARK }

  /** Delivery cursor of a pulled record (servers without it fall back to `updated_at`). */
  private fun deliveryOf(item: ClienteComFotosDto): Long =
    item.client.deliveryTs ?: item.client.updatedAt

  /** Delivery cursor of a tombstone (servers without it fall back to `deleted_at`). */
  private fun deliveryOf(tombstone: TombstoneDto): Long =
    tombstone.deliveryTs ?: tombstone.deletedAt

  private companion object {
    /** Records requested per pull page (the server accepts up to 1000). */
    const val PULL_PAGE_SIZE = 200

    /** Pause before retrying a failed photo download. */
    const val PHOTO_RETRY_DELAY_MS = 500L

    /**
     * How long a pending record may hold the pull watermark. Past this age the
     * record is assumed to be stuck (the server keeps rejecting it) and is
     * skipped by the cursor instead of re-downloading everything after it.
     */
    const val WATERMARK_HOLD_SECONDS = 7L * 24 * 60 * 60
  }
}
