package com.example.elisdb.sync

import com.example.elisdb.data.local.ClienteDao
import com.example.elisdb.remote.ConfigException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Provides the coordinator; abstracted so tests can substitute one. */
fun interface SyncCoordinatorProvider {
  suspend fun coordinator(): SyncCoordinator
}

/**
 * Owns manual sync execution and exposes a live status plus the
 * pending-uploads / pending-downloads signals (when a [ClienteDao] and a
 * watermark/fetch are supplied).
 */
class SyncManager(
  private val coordinatorProvider: SyncCoordinatorProvider,
  private val runScope: CoroutineScope = CoroutineScope(SupervisorJob()),
  private val clienteDao: ClienteDao? = null,
  private val pullWatermark: PullWatermark? = null,
  private val fetchServerInfo: suspend () -> ServerInfo? = { null },
  private val lastSyncStore: LastSyncStore? = null,
) {
  private val _status = MutableStateFlow(SyncStatus.IDLE)
  val status: StateFlow<SyncStatus> = _status.asStateFlow()

  private val _fault = MutableStateFlow<SyncFault?>(null)
  val fault: StateFlow<SyncFault?> = _fault.asStateFlow()

  private val _lastResult = MutableStateFlow<String?>(null)
  val lastResult: StateFlow<String?> = _lastResult.asStateFlow()

  private val _hasPendingUploads = MutableStateFlow(false)
  val hasPendingUploads: StateFlow<Boolean> = _hasPendingUploads.asStateFlow()

  private val _pendingUploadCount = MutableStateFlow(0)

  /** Number of local records (created/edited/deleted) waiting to be sent. */
  val pendingUploadCount: StateFlow<Int> = _pendingUploadCount.asStateFlow()

  private val _hasPendingDownloads = MutableStateFlow(false)
  val hasPendingDownloads: StateFlow<Boolean> = _hasPendingDownloads.asStateFlow()

  private val _serverClienteCount = MutableStateFlow<Int?>(null)

  /** Number of clients the server stores, or null when never fetched/unreachable. */
  val serverClienteCount: StateFlow<Int?> = _serverClienteCount.asStateFlow()

  private val _lastSuccessAt = MutableStateFlow(lastSyncStore?.getLastSuccessAt())

  /** Epoch seconds of the last successful sync, or null when never completed. */
  val lastSuccessAt: StateFlow<Long?> = _lastSuccessAt.asStateFlow()

  private val mutex = Mutex()

  /** The in-flight run, if any; concurrent triggers join it instead of starting a second run. */
  private var inFlight: Deferred<SyncOutcome>? = null

  init {
    val dao = clienteDao
    if (dao != null) {
      runScope.launch {
        // Include locally-deleted (pending-deletion) records so the sync status
        // reflects uploads that are still waiting to be sent to the server.
        dao.observeAllIncludingDeleted().collect { clients ->
          val pending = clients.count { it.pending || it.deletePending }
          _pendingUploadCount.value = pending
          _hasPendingUploads.value = pending > 0
        }
      }
    }
  }

  /**
   * Refreshes [hasPendingDownloads] and [serverClienteCount] from the server's
   * status response (`/api/latest`) compared with the persisted pull watermark.
   * Best-effort: failures leave the last values.
   */
  suspend fun refreshPendingDownloads() {
    val watermark = pullWatermark ?: return
    val info = try {
      fetchServerInfo()
    } catch (e: Exception) {
      null
    } ?: return
    _serverClienteCount.value = info.clientCount
    val current = watermark.get()
    // With no local watermark (fresh install / never pulled), any server data
    // (latest > 0) is still to be received. Once a watermark exists, any
    // difference counts: a server restored from an older backup reports a
    // latest below the local cursor and still has data to re-download.
    _hasPendingDownloads.value =
      if (current == null) info.latestUpdatedAt > 0 else info.latestUpdatedAt != current
  }

  /**
   * Forgets the observations tied to the previous server: the last status,
   * fault, result and last-success time. The stored settings are not touched
   * here; the caller also clears the persisted watermark, photo hashes and
   * clock offset.
   */
  fun resetState() {
    _status.value = SyncStatus.IDLE
    _fault.value = null
    _lastResult.value = null
    _lastSuccessAt.value = null
    _serverClienteCount.value = null
    _hasPendingDownloads.value = false
  }

  /**
   * Runs exactly one sync (single-flight): concurrent callers join the in-flight
   * run and observe its outcome. Publishes [SyncStatus]/[SyncFault] for every
   * execution.
   *
   * Publication happens in [runScope] (see [startRun]), so the observable status
   * reaches a terminal state even when the calling coroutine is cancelled while
   * awaiting — e.g. the user leaves the screen that triggered the sync.
   */
  suspend fun sync(): SyncOutcome {
    // Entry is non-cancellable: a caller cancelled between setting IN_PROGRESS
    // and starting/joining the run must not leave the status stuck in
    // IN_PROGRESS. Cancellation still takes effect at run.await() below.
    val run =
      withContext(NonCancellable) {
        _status.value = SyncStatus.IN_PROGRESS
        _fault.value = null
        _lastResult.value = null
        mutex.withLock {
          val existing = inFlight
          if (existing != null && existing.isActive) {
            existing
          } else {
            startRun().also { inFlight = it }
          }
        }
      }
    try {
      return run.await()
    } finally {
      // Best-effort: drop the finished run so the next trigger starts fresh.
      // A skipped cleanup is harmless — joiners only join while the run is
      // active, and a completed reference is replaced on the next call.
      if (!run.isActive) {
        mutex.withLock { if (inFlight === run) inFlight = null }
      }
    }
  }

  /** Runs a sync attempt in a background scope, updating [status]. */
  suspend fun syncNow(): SyncStatus {
    val outcome = sync()
    return mapOutcome(outcome).status
  }

  /**
   * Runs a sync attempt in the background scope, updating [status].
   *
   * Publication (and the post-success bookkeeping) lives inside the run's own
   * coroutine so it never depends on a caller's lifecycle: awaiting callers may
   * be cancelled (navigation, scope teardown) while the transfer keeps running,
   * and the observable status must still reach a terminal state.
   */
  private fun startRun(): Deferred<SyncOutcome> =
    runScope.async {
      // Take the server snapshot before pushing/pulling. The pull writes rows
      // into Room, so without this the local count starts changing while the
      // server count is still unknown; this way both counts are available in
      // the order the user expects.
      refreshPendingDownloads()
      val outcome =
        try {
          coordinatorProvider.coordinator().sync()
        } catch (e: ConfigException) {
          android.util.Log.w("ElisdbSync", "sync run aborted: configuration problem", e)
          SyncOutcome.ConfigFault
        } catch (e: Exception) {
          // A transient fault is surfaced to the user as a generic network
          // message, so log the cause to keep it diagnosable.
          android.util.Log.w("ElisdbSync", "sync run failed", e)
          SyncOutcome.TransientError
        }
      publish(outcome)
      if (_status.value == SyncStatus.SUCCESS) {
        val now = System.currentTimeMillis() / 1000
        _lastSuccessAt.value = now
        lastSyncStore?.setLastSuccessAt(now)
        refreshPendingDownloads()
      }
      outcome
    }

  private fun publish(outcome: SyncOutcome) {
    val display = mapOutcome(outcome)
    display.fault?.let { _fault.value = it }
    display.lastResult?.let { _lastResult.value = it }
    _status.value = display.status
  }

  /** Maps a sync outcome to a status + optional diagnostic, so it is unit-testable. */
  internal fun mapOutcome(outcome: SyncOutcome): OutcomeDisplay =
    Companion.mapOutcome(outcome)

  internal data class OutcomeDisplay(
    val status: SyncStatus,
    val fault: SyncFault? = null,
    val lastResult: String? = null,
  )

  companion object {
    internal fun mapOutcome(outcome: SyncOutcome): OutcomeDisplay =
      when (outcome) {
        SyncOutcome.Success -> OutcomeDisplay(SyncStatus.SUCCESS)
        is SyncOutcome.SuccessWithStatuses ->
          if (outcome.stale.isEmpty()) OutcomeDisplay(SyncStatus.SUCCESS)
          else
            OutcomeDisplay(
              SyncStatus.SUCCESS,
              lastResult =
                "As tuas edições de «${outcome.stale.joinToString("», «")}» foram substituídas por versões mais recentes no servidor.",
            )
        is SyncOutcome.DuplicateMerged ->
          OutcomeDisplay(SyncStatus.SUCCESS, lastResult = "Registo «${outcome.nome}» foi fundido com um registo existente.")
        SyncOutcome.AuthFailure -> OutcomeDisplay(SyncStatus.ERROR, SyncFault.AUTH)
        SyncOutcome.ClockFailure -> OutcomeDisplay(SyncStatus.ERROR, SyncFault.CLOCK)
        SyncOutcome.TransientError -> OutcomeDisplay(SyncStatus.ERROR, SyncFault.NETWORK)
        SyncOutcome.ConfigFault -> OutcomeDisplay(SyncStatus.ERROR, SyncFault.CONFIG)
      }
  }
}
