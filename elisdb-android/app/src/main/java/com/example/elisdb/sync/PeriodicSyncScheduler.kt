package com.example.elisdb.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Owns the WorkManager jobs for background sync:
 * - one unique periodic job when the user enables automatic sync;
 * - one unique one-off retry job after a transient failure.
 *
 * The periodic interval is the WorkManager minimum (15 minutes). Manual syncs
 * bypass this scheduler and run immediately.
 */
class PeriodicSyncScheduler(
  context: Context,
  private val settings: PeriodicSyncSettings,
  private val isConfigured: () -> Boolean,
) {
  private val workManager = WorkManager.getInstance(context)

  fun scheduleIfEnabled() {
    if (settings.isEnabled() && isConfigured()) {
      enqueuePeriodic()
    } else {
      workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
      if (!isConfigured()) workManager.cancelUniqueWork(RETRY_WORK_NAME)
    }
  }

  /** Cancels a pending one-off retry after a successful sync (manual or periodic). */
  fun cancelRetry() {
    workManager.cancelUniqueWork(RETRY_WORK_NAME)
  }

  fun setEnabled(enabled: Boolean) {
    settings.setEnabled(enabled)
    if (enabled && isConfigured()) {
      enqueuePeriodic()
    } else {
      workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }
    if (!enabled) workManager.cancelUniqueWork(RETRY_WORK_NAME)
  }

  /** Schedules a one-off retry with exponential backoff after a transient failure. */
  fun scheduleRetry() {
    if (!isConfigured()) return
    val request =
      OneTimeWorkRequestBuilder<PeriodicSyncWorker>()
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
        .build()
    // KEEP preserves an already pending retry (and its accumulated backoff)
    // instead of resetting it on every failed attempt.
    workManager.enqueueUniqueWork(RETRY_WORK_NAME, ExistingWorkPolicy.KEEP, request)
  }

  private fun enqueuePeriodic() {
    val request =
      PeriodicWorkRequestBuilder<PeriodicSyncWorker>(PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES)
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
        .build()
    // UPDATE applies a new interval/config without duplicating the job.
    workManager.enqueueUniquePeriodicWork(
      PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }

  private fun networkConstraints(): Constraints =
    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

  private companion object {
    const val PERIODIC_WORK_NAME = "elisdb-periodic-sync"
    const val RETRY_WORK_NAME = "elisdb-sync-retry"
    const val PERIODIC_INTERVAL_MINUTES = 15L
    const val BACKOFF_SECONDS = 30L
  }
}
