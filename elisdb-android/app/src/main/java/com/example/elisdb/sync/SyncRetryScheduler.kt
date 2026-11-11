package com.example.elisdb.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the single background retry that follows a transient network or
 * server failure. The app no longer runs a periodic sync: transfers happen on
 * launch, after a save/edit and on the manual action. Transient failures still
 * get one retry with exponential backoff, as required by the resilience
 * contract; auth/clock/config failures are never retried automatically.
 */
class SyncRetryScheduler(
  context: Context,
  private val isConfigured: () -> Boolean,
) {
  private val workManager = WorkManager.getInstance(context.applicationContext)

  /**
   * Cancels the 15-minute job enqueued by versions that offered the optional
   * automatic sync, so an upgrade stops the schedule even if it was enabled.
   */
  fun cancelLegacyPeriodicSync() {
    workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
  }

  fun cancelRetry() {
    workManager.cancelUniqueWork(RETRY_WORK_NAME)
  }

  /** Drops retries and legacy periodic work, e.g. when the server changes. */
  fun cancelPending() {
    cancelRetry()
    cancelLegacyPeriodicSync()
  }

  fun scheduleRetry() {
    if (!isConfigured()) return
    val request =
      OneTimeWorkRequestBuilder<PeriodicSyncWorker>()
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
        .build()
    workManager.enqueueUniqueWork(RETRY_WORK_NAME, ExistingWorkPolicy.KEEP, request)
  }

  private fun networkConstraints(): Constraints =
    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

  private companion object {
    /** Name used by the retired 15-minute periodic job. */
    const val PERIODIC_WORK_NAME = "elisdb-periodic-sync"
    const val RETRY_WORK_NAME = "elisdb-sync-retry"
    const val BACKOFF_SECONDS = 30L
  }
}
