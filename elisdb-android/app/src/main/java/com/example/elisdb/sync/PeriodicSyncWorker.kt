package com.example.elisdb.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.elisdb.ElisdbApp

/**
 * Runs one background retry attempt. It joins an in-flight run through
 * [SyncManager.sync] (single-flight), so a retry never duplicates a manual or
 * save-triggered transfer.
 */
class PeriodicSyncWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    val app = applicationContext as? ElisdbApp ?: return Result.success()
    val container = app.container
    val settings = container.settingsStore.settings()
    if (settings.baseUrl.isBlank() || settings.signingKey.isBlank()) {
      return Result.success()
    }
    return resultFor(container.syncManager.sync())
  }

  companion object {
    /**
     * Transient failures are retried by WorkManager's exponential backoff.
     * Auth/clock/config failures need the user to fix the device, so retrying
     * them would only burn battery.
     */
    fun resultFor(outcome: SyncOutcome): Result =
      when (outcome) {
        SyncOutcome.Success,
        is SyncOutcome.SuccessWithStatuses,
        is SyncOutcome.SuccessReport,
        is SyncOutcome.DuplicateMerged -> Result.success()
        SyncOutcome.TransientError -> Result.retry()
        SyncOutcome.AuthFailure,
        SyncOutcome.ClockFailure,
        SyncOutcome.ConfigFault -> Result.failure()
      }
  }
}
