package com.example.elisdb.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.elisdb.ElisdbApp
import com.example.elisdb.data.local.ClienteDao
import com.example.elisdb.remote.ConfigException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Provides the coordinator; abstracted so tests can substitute one. */
fun interface SyncCoordinatorProvider {
  suspend fun coordinator(): SyncCoordinator
}

/**
 * Owns the periodic/manual sync schedule and exposes a live status plus the
 * pending-uploads / pending-downloads signals (when a [ClienteDao] and a
 * watermark/fetch are supplied).
 */
class SyncManager(
  private val coordinatorProvider: SyncCoordinatorProvider,
  private val workManager: WorkManager? = null,
  private val runScope: CoroutineScope = CoroutineScope(SupervisorJob()),
  private val clienteDao: ClienteDao? = null,
  private val pullWatermark: PullWatermark? = null,
  private val fetchLatestUpdatedAt: suspend () -> Long? = { null },
) {
  private val _status = MutableStateFlow(SyncStatus.IDLE)
  val status: StateFlow<SyncStatus> = _status.asStateFlow()

  private val _fault = MutableStateFlow<SyncFault?>(null)
  val fault: StateFlow<SyncFault?> = _fault.asStateFlow()

  private val _lastResult = MutableStateFlow<String?>(null)
  val lastResult: StateFlow<String?> = _lastResult.asStateFlow()

  private val _hasPendingUploads = MutableStateFlow(false)
  val hasPendingUploads: StateFlow<Boolean> = _hasPendingUploads.asStateFlow()

  private val _hasPendingDownloads = MutableStateFlow(false)
  val hasPendingDownloads: StateFlow<Boolean> = _hasPendingDownloads.asStateFlow()

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
          _hasPendingUploads.value = clients.any { it.pending || it.deletePending }
        }
      }
    }
  }

  /**
   * Refreshes [hasPendingDownloads] from the server's latest timestamp vs the
   * persisted pull watermark. Best-effort: failures leave the last value.
   */
  suspend fun refreshPendingDownloads() {
    val watermark = pullWatermark ?: return
    val latest = try {
      fetchLatestUpdatedAt()
    } catch (e: Exception) {
      null
    } ?: return
    val current = watermark.get()
    // With no local watermark (fresh install / never pulled), any server data
    // (latest > 0) is still to be received. Once a watermark exists, only a
    // newer server timestamp counts.
    _hasPendingDownloads.value = if (current == null) latest > 0 else latest > current
  }

  fun schedulePeriodic() {
    val wm = workManager ?: return
    val constraints =
      Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val request =
      PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    wm.enqueueUniquePeriodicWork(
      PERIODIC_WORK,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }

  /**
   * Runs exactly one sync (single-flight): concurrent callers join the in-flight
   * run and observe its outcome. Publishes [SyncStatus]/[SyncFault] for every
   * execution, including those started by the periodic [SyncWorker].
   */
  suspend fun sync(): SyncOutcome {
    _status.value = SyncStatus.IN_PROGRESS
    _fault.value = null
    _lastResult.value = null
    val (created, run) =
      mutex.withLock {
        val existing = inFlight
        if (existing != null && existing.isActive) {
          false to existing
        } else {
          val fresh = startRun()
          inFlight = fresh
          true to fresh
        }
      }
    val outcome =
      try {
        run.await()
      } finally {
        if (created) {
          mutex.withLock { if (inFlight === run) inFlight = null }
        }
      }
    publish(outcome)
    if (outcome is SyncOutcome.Success || outcome is SyncOutcome.SuccessWithStatuses) {
      refreshPendingDownloads()
    }
    return outcome
  }

  /** Runs a sync attempt in a background scope, updating [status]. */
  suspend fun syncNow(): SyncStatus {
    val outcome = sync()
    return mapOutcome(outcome).status
  }

  private fun startRun(): Deferred<SyncOutcome> =
    runScope.async {
      try {
        coordinatorProvider.coordinator().sync()
      } catch (e: ConfigException) {
        SyncOutcome.ConfigFault
      } catch (e: Exception) {
        SyncOutcome.TransientError
      }
    }

  private fun publish(outcome: SyncOutcome) {
    when (val display = mapOutcome(outcome)) {
      is OutcomeDisplay -> {
        display.fault?.let { _fault.value = it }
        display.lastResult?.let { _lastResult.value = it }
        _status.value = display.status
      }
    }
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
    private const val PERIODIC_WORK = "elisdb_sync"

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

/** WorkManager worker that performs a sync attempt in the background. */
class SyncWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    val app = applicationContext as? ElisdbApp ?: return Result.retry()
    return mapToResult(app.container.syncManager.sync())
  }

  companion object {
    /** Maps a sync outcome to the WorkManager schedule decision, so it is unit-testable. */
    internal fun mapToResult(outcome: SyncOutcome): ListenableWorker.Result =
      when (outcome) {
        SyncOutcome.Success, is SyncOutcome.SuccessWithStatuses, is SyncOutcome.DuplicateMerged -> ListenableWorker.Result.success()
        SyncOutcome.AuthFailure -> ListenableWorker.Result.failure() // don't retry indefinitely
        SyncOutcome.ClockFailure -> ListenableWorker.Result.failure() // device clock won't self-correct
        SyncOutcome.TransientError -> ListenableWorker.Result.retry()
        SyncOutcome.ConfigFault -> ListenableWorker.Result.failure() // configuration won't fix itself
      }
  }
}