package com.example.elisdb.sync

import androidx.work.ListenableWorker
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodicSyncWorkerTest {
  @Test
  fun `transient failures ask WorkManager to retry`() {
    assertEquals(
      ListenableWorker.Result.retry(),
      PeriodicSyncWorker.resultFor(SyncOutcome.TransientError),
    )
  }

  @Test
  fun `auth clock and config failures stop automatic retries`() {
    assertEquals(
      ListenableWorker.Result.failure(),
      PeriodicSyncWorker.resultFor(SyncOutcome.AuthFailure),
    )
    assertEquals(
      ListenableWorker.Result.failure(),
      PeriodicSyncWorker.resultFor(SyncOutcome.ClockFailure),
    )
    assertEquals(
      ListenableWorker.Result.failure(),
      PeriodicSyncWorker.resultFor(SyncOutcome.ConfigFault),
    )
  }

  @Test
  fun `successful outcomes finish the job`() {
    listOf(
      SyncOutcome.Success,
      SyncOutcome.SuccessWithStatuses(accepted = 1, stale = emptyList()),
      SyncOutcome.SuccessReport(),
      SyncOutcome.DuplicateMerged("Maria"),
    ).forEach { outcome ->
      assertEquals(ListenableWorker.Result.success(), PeriodicSyncWorker.resultFor(outcome))
    }
  }
}
