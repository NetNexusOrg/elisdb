package com.example.elisdb.sync

import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteDao
import com.example.elisdb.data.local.Foto
import com.example.elisdb.data.local.FotoDao
import com.example.elisdb.remote.AuthCause
import com.example.elisdb.remote.AuthException
import com.example.elisdb.remote.ClienteComFotosDto
import com.example.elisdb.remote.ConfigException
import com.example.elisdb.remote.RecordDto
import com.example.elisdb.remote.ServerException
import com.example.elisdb.remote.TombstoneDto
import java.security.MessageDigest
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
  private val serverIdentityStore: ServerIdentityStore? = null,
  private val maxPhotosPerClient: Int = 500,
  private val onWarning: (String) -> Unit = {},
  /**
   * Receives the unexpected exception that aborts a run. The UI only shows a
   * generic network message, so this keeps the real cause diagnosable.
   */
  private val onError: (Throwable) -> Unit = {},
  /** Releases the API client lease after a run, when one was supplied. */
  private val onFinished: () -> Unit = {},
) {
  /** Device clock (epoch seconds) shifted by the last-known server offset, when available. */
  private fun normalizedEpochSeconds(): Long =
    System.currentTimeMillis() / 1000 + (clockOffsetStore?.getOffsetSeconds() ?: 0L)

  suspend fun sync(): SyncOutcome =
    try {
      refreshClockOffset()
      val push = pushEdits()
      pushDeletions()
      val pull = pull()
      when {
        push.mergedNome != null && push.warnings.isEmpty() && !pull.identityChanged ->
          SyncOutcome.DuplicateMerged(push.mergedNome)
        push.staleNames.isNotEmpty() && push.warnings.isEmpty() && !pull.identityChanged ->
          SyncOutcome.SuccessWithStatuses(accepted = 0, stale = push.staleNames)
        push.warnings.isNotEmpty() || pull.identityChanged ->
          SyncOutcome.SuccessReport(
            warnings = push.warnings,
            identityChanged = pull.identityChanged,
            stale = push.staleNames,
          )
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
    } finally {
      onFinished()
    }

  /**
   * Explicit, user-confirmed replacement of local data by a complete server
   * dataset. A snapshot callback MUST succeed before anything is removed; the
   * caller is responsible for obtaining confirmation first.
   */
  suspend fun replaceLocalWithServer(snapshot: suspend () -> Unit): SyncOutcome =
    try {
      snapshot()
      refreshClockOffset()
      pull(destructive = true, forceFull = true)
      SyncOutcome.Success
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
    } finally {
      onFinished()
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

  private data class PushRecord(
    val cliente: Cliente,
    val record: RecordDto,
    val hashes: Map<String, String>,
    val fotos: List<Foto>,
  )

  private data class PushReport(
    val staleNames: List<String> = emptyList(),
    val mergedNome: String? = null,
    val warnings: List<String> = emptyList(),
  )

  /**
   * Sends pending edits in bounded batches.
   *
   * Photos are referenced by metadata first; the server answers with
   * `missing_photos`, and only those bytes are uploaded afterwards. A rejected
   * batch therefore never leaves uploaded files orphaned, and one bad record
   * cannot block every later batch: a 422 is retried with a smaller batch and
   * eventually isolated to the single offending record.
   */
  private suspend fun pushEdits(): PushReport {
    val pending = clienteDao.getPending().filter { !it.deletePending }
    if (pending.isEmpty()) return PushReport()

    val warnings = mutableListOf<String>()
    val batchRecords = mutableListOf<PushRecord>()
    for (cliente in pending) {
      val fotos = fotoDao.getByCliente(cliente.id).filter { !it.deletePending }
      if (fotos.size > maxPhotosPerClient) {
        val message =
          "O cliente «${cliente.nome}» tem ${fotos.size} fotos; o máximo por cliente é " +
            "$maxPhotosPerClient. Remove fotos antes de sincronizar."
        warnings += message
        onWarning(message)
        continue
      }

      val hashes = mutableMapOf<String, String>()
      var missing: Foto? = null
      for (foto in fotos) {
        // Read one photo at a time: never keep the whole client's photos in
        // memory (a client may legitimately have hundreds).
        val bytes = photoStore.readBytes(foto.uriLocal)
        if (bytes == null) {
          missing = foto
          break
        }
        hashes[foto.id] = PhotoHashes.sha256(bytes)
      }
      if (missing != null) {
        // Never push a record that silently omits a photo: the server would
        // treat the omission as a removal and every device would delete its
        // copy. Keep the client pending and retry on a later sync.
        val message =
          "Foto ${missing.id} (cliente ${cliente.id}) sem ficheiro; sincronização do cliente adiada"
        warnings += message
        onWarning(message)
        continue
      }
      batchRecords += PushRecord(cliente, SyncMapper.toRecord(cliente, fotos), hashes, fotos)
    }
    if (batchRecords.isEmpty()) return PushReport(warnings = warnings)

    var batchSize = effectivePushBatchSize()
    var index = 0
    val staleIds = mutableListOf<String>()
    val staleNames = mutableListOf<String>()
    var mergedNome: String? = null
    val filesToDelete = mutableSetOf<String>()

    while (index < batchRecords.size) {
      val chunk = batchRecords.subList(index, minOf(index + batchSize, batchRecords.size)).toList()
      val results =
        try {
          api.push(
            chunk.map { it.record },
            timestamp = normalizedEpochSeconds(),
            idempotencyKey = pushIdempotencyKey(chunk),
          )
        } catch (e: AuthException) {
          throw e
        } catch (e: ConfigException) {
          throw e
        } catch (e: ServerException) {
          if (e.status == 422 && chunk.size > 1) {
            // The server may enforce a lower limit than it advertised (or reject
            // one malformed record). Halve the batch until the bad record is
            // isolated instead of leaving every record stuck.
            batchSize = maxOf(1, chunk.size / 2)
            val message = "O servidor rejeitou um lote de ${chunk.size} registos; a reduzir para $batchSize"
            warnings += message
            onWarning(message)
            continue
          }
          val message = "Lote de ${chunk.size} registos rejeitado pelo servidor (${e.status}); ficam pendentes"
          warnings += message
          onWarning(message)
          index += chunk.size
          continue
        } catch (e: Exception) {
          val message = "Falha ao enviar um lote de ${chunk.size} registos; ficam pendentes"
          warnings += message
          onWarning(message)
          index += chunk.size
          continue
        }

      val byId = results.associateBy { it.id }
      runInTransaction {
        chunk.forEach { item ->
          val result = byId[item.cliente.id]
          when {
            result == null -> {
              val message = "Servidor não devolveu resultado para ${item.cliente.id}; mantido pendente"
              warnings += message
              onWarning(message)
            }
            result.status == "duplicate" && result.existingId != null -> {
              // The server already has a record with the same normalized name.
              // Adopt the canonical id but keep the user's local edits pending so
              // they re-push and the server stores the merged values (LWW).
              filesToDelete += mergeDuplicate(item.cliente, result)
              mergedNome = item.cliente.nome
            }
            result.status == "stale" -> {
              // The server has a newer version. Do NOT clear the watermark here:
              // the follow-up refresh below reads the server version directly.
              if (clienteDao.markSynced(item.cliente.id, item.cliente.revision) == 1) {
                staleIds += item.cliente.id
                staleNames += item.cliente.nome
              } else {
                val message =
                  "O cliente ${item.cliente.nome} mudou durante a sincronização; a alteração será reenviada"
                warnings += message
                onWarning(message)
              }
            }
            result.status == "error" -> {
              val message = "Servidor rejeitou o registo ${item.cliente.id}; mantido pendente"
              warnings += message
              onWarning(message)
            }
            else -> {
              val missing = result.missingPhotos.toSet()
              if (missing.isEmpty()) {
                if (clienteDao.markSynced(item.cliente.id, item.cliente.revision) == 1) {
                  recordPushedHashes(item.cliente.id, item.hashes)
                }
              } else {
                // The metadata was accepted, but the server still needs bytes.
                // Upload them after this transaction commits.
                // (handled below, outside the transaction)
              }
            }
          }
        }
      }

      // Upload only the photos the server reported as missing, after the
      // metadata was accepted. A rejected batch never reaches this point.
      chunk.forEach { item ->
        val result = byId[item.cliente.id] ?: return@forEach
        if (result.status != "accepted" && result.status != "stale" && result.status != "duplicate") {
          return@forEach
        }
        val missing = result.missingPhotos.toSet()
        if (missing.isEmpty()) return@forEach
        val toUpload = item.fotos.filter { it.id in missing }
        var uploadedAll = true
        val uploadedHashes = mutableMapOf<String, String>()
        for (foto in toUpload) {
          val bytes = photoStore.readBytes(foto.uriLocal)
          if (bytes == null) {
            val message = "Foto ${foto.id} desapareceu antes do upload; cliente ${item.cliente.id} mantido pendente"
            warnings += message
            onWarning(message)
            uploadedAll = false
            continue
          }
          try {
            api.uploadPhoto(foto.id, bytes, timestamp = normalizedEpochSeconds())
            uploadedHashes[foto.id] = item.hashes[foto.id] ?: PhotoHashes.sha256(bytes)
          } catch (e: AuthException) {
            throw e
          } catch (e: ConfigException) {
            throw e
          } catch (e: Exception) {
            val message = "Falha ao enviar a foto ${foto.id}; cliente ${item.cliente.id} mantido pendente"
            warnings += message
            onWarning(message)
            uploadedAll = false
          }
        }
        val hasAllMissing = missing.all { it in item.hashes }
        if (uploadedAll && hasAllMissing) {
          runInTransaction {
            if (clienteDao.markSynced(item.cliente.id, item.cliente.revision) == 1) {
              recordPushedHashes(item.cliente.id, item.hashes)
            }
          }
        } else {
          // Record what did reach the server so a retry only uploads the rest;
          // the client remains pending until every referenced photo is stored.
          if (uploadedHashes.isNotEmpty()) recordPushedHashes(item.cliente.id, uploadedHashes)
        }
      }

      index += chunk.size
    }

    // Superseded photo files are removed only after the push transactions have
    // committed, so a later failure cannot leave rows pointing at deleted files.
    filesToDelete.forEach { photoStore.delete(it) }

    if (staleIds.isNotEmpty()) refreshStaleClients(staleIds, warnings)
    return PushReport(staleNames = staleNames, mergedNome = mergedNome, warnings = warnings)
  }

  /**
   * Stable key for one push batch: identical content reuses the key across
   * retries, while any edit to a record or its photo metadata changes it.
   */
  private fun pushIdempotencyKey(chunk: List<PushRecord>): String {
    val canonical = buildString {
      chunk.forEach { item ->
        append(item.record.client.id).append('\u001f')
        append(item.record.client.updatedAt).append('\u001f')
        item.record.fotos.forEach { foto ->
          append(foto.id).append('\u001e')
          append(foto.mime).append('\u001e')
          append(foto.ordem).append('\u001d')
        }
        append('\u001c')
      }
    }
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(canonical.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  /**
   * The effective batch size: the conservative client default, capped by the
   * server's advertised limit when available. Old servers do not advertise it;
   * 100 is below their default 200, so the first push still fits.
   */
  private suspend fun effectivePushBatchSize(): Int {
    val info = runCatching { api.fetchServerInfo(timestamp = normalizedEpochSeconds()) }.getOrNull()
    val serverMax = info?.maxPushRecords
    return if (serverMax != null) minOf(DEFAULT_PUSH_BATCH_SIZE, serverMax).coerceAtLeast(1)
    else DEFAULT_PUSH_BATCH_SIZE
  }

  /**
   * Refreshes records the server rejected as stale from `/api/clients`, so the
   * user sees the winning server version without clearing the pull watermark
   * (which used to trigger a full, formerly destructive, pull).
   */
  private suspend fun refreshStaleClients(ids: List<String>, warnings: MutableList<String>) {
    val serverClients =
      try {
        api.fetchClients(timestamp = normalizedEpochSeconds())
      } catch (e: AuthException) {
        throw e
      } catch (e: ConfigException) {
        throw e
      } catch (e: Exception) {
        val message = "Não foi possível atualizar os registos rejeitados como antigos; serão atualizados no próximo pull"
        warnings += message
        onWarning(message)
        return
      }
    val byId = serverClients.associateBy { it.id }
    runInTransaction {
      ids.forEach { id ->
        val local = clienteDao.getById(id) ?: return@forEach
        if (local.pending || local.deletePending) return@forEach
        val server = byId[id]
        if (server == null) {
          // The server no longer has this record. Keep it visibly local-only
          // instead of deleting it; the user decides what to do.
          if (!local.localOnly) clienteDao.update(local.copy(localOnly = true))
          return@forEach
        }
        val createdAt = server.createdAt ?: local.createdAt
        clienteDao.update(
          local.copy(
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
            localOnly = false,
          )
        )
      }
    }
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
          localOnly = false,
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

  private data class PullReport(val identityChanged: Boolean = false)

  private suspend fun pull(destructive: Boolean = false, forceFull: Boolean = false): PullReport {
    PhotoStoreGuard.mutex.withLock {
      var watermark = if (incrementalPullEnabled && !forceFull) pullWatermark?.get() else null
      var identityChanged = false

      // Always ask for the server status: it carries the stable instance id and
      // the batch limit. Best effort, so an old server that does not expose
      // these fields keeps working.
      val info =
        try {
          api.fetchServerInfo(timestamp = normalizedEpochSeconds())
        } catch (e: kotlinx.coroutines.CancellationException) {
          throw e
        } catch (_: Exception) {
          null
        }

      val currentIdentity = info?.instanceId
      if (currentIdentity != null) {
        val storedIdentity = serverIdentityStore?.get()
        when {
          storedIdentity == null -> serverIdentityStore?.set(currentIdentity)
          storedIdentity != currentIdentity -> {
            identityChanged = true
            serverIdentityStore.set(currentIdentity)
            // The cursor belongs to the previous server and is meaningless now.
            // A full pull follows, but it is non-destructive: local records not
            // on the new server are preserved as local-only.
            pullWatermark?.clear()
            watermark = null
            onWarning(
              "O servidor mudou (ou foi restaurado). Os dados locais serão preservados; confirma no ecrã de configuração se quiseres substituí-los pelos do servidor."
            )
          }
        }
      }

      // A server restored from an older backup can report a latest timestamp
      // below the local cursor; a delta pull would then never return the
      // restored records. Detect it and fall back to a (non-destructive) full pull.
      val latest = info?.latestUpdatedAt
      if (watermark != null && latest != null && latest < watermark) {
        pullWatermark?.clear()
        watermark = null
        onWarning(
          "O servidor parece ter sido restaurado de uma cópia mais antiga; os dados locais serão preservados e sincronizados de novo"
        )
      }

      val isFullPull = watermark == null
      // Follow the server's pagination cursor to the last page before applying,
      // so a paginated pull never reconciles based on a partial page.
      var since = watermark
      var sinceId: String? = null
      val allServerClients = mutableListOf<ClienteComFotosDto>()
      val allTombstones = mutableListOf<TombstoneDto>()
      var paginationComplete = true
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
          paginationComplete = false
          onWarning("Cursor de paginação não avançou; o pull foi interrompido sem remover dados locais")
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
        // Tombstoned records are deleted locally unless they have pending edits
        // or a newer local edit, so a delete made on another device propagates
        // even on delta pulls without discarding newer work.
        allTombstones.forEach { tombstone ->
          val local = clienteDao.getById(tombstone.id)
          when {
            local == null -> Unit
            local.deletePending -> {
              // The local deletion is already queued; let the delete push
              // finish it instead of finalizing from a remote marker.
            }
            local.pending -> {
              if (holdsWatermark(local)) skippedDelivery += deliveryOf(tombstone)
            }
            local.updatedAt > tombstone.deletedAt -> {
              // The local edit is newer than the deletion. Keep it, mark it for
              // re-push, and hold the cursor so the delete is evaluated again
              // after the push (which resurrects the record on the server).
              clienteDao.update(
                local.copy(
                  pending = true,
                  localOnly = false,
                  revision = local.revision + 1,
                )
              )
              skippedDelivery += deliveryOf(tombstone)
            }
            else -> filesToDelete += finalizeDeletion(tombstone.id)
          }
        }

        // Reconcile local records that the server does not have. This is only
        // safe after a completed full pull. By default the records are kept and
        // marked local-only; only an explicit user-confirmed replacement may
        // delete them.
        if (isFullPull) {
          if (paginationComplete) {
            val serverIds = allServerClients.mapTo(mutableSetOf()) { it.client.id }
            clienteDao.getAll().forEach { local ->
              val shouldBeLocalOnly =
                local.id !in serverIds && !local.pending && !local.deletePending
              if (local.localOnly != shouldBeLocalOnly) {
                clienteDao.update(local.copy(localOnly = shouldBeLocalOnly))
              }
              if (destructive && shouldBeLocalOnly) {
                filesToDelete += finalizeDeletion(local.id)
              }
            }
          } else {
            onWarning(
              "Pull incompleto: os registos locais ausentes do servidor foram preservados"
            )
          }
        }

        // Advance the watermark to the largest delivery timestamp seen on this
        // pull (the server's cursor, not the device clocks), holding it below the
        // oldest record that was skipped because of a pending local edit. An
        // incomplete pull never advances the cursor, or it could skip records.
        if (paginationComplete) {
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
      }
      // Photo files removed by the deletions above are deleted only after the
      // transaction has committed, so a rollback cannot leave rows pointing at
      // deleted files.
      filesToDelete.forEach { photoStore.delete(it) }

      // Reclaim files left over after a successful pull, inside the same guard
      // that serializes materialization vs. any app-start sweep (R8).
      photoSweeper?.sweep()
      return PullReport(identityChanged = identityChanged)
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
        localOnly = false,
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

    /**
     * Conservative push batch size when the server does not advertise one.
     * The server default is 200, so this still fits an older server.
     */
    const val DEFAULT_PUSH_BATCH_SIZE = 100

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
