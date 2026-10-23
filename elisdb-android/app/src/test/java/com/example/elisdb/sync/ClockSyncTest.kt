package com.example.elisdb.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockSyncTest {
  private class Store(var offset: Long? = null) : ClockOffsetStore {
    override fun getOffsetSeconds(): Long? = offset

    override fun setOffsetSeconds(offsetSeconds: Long) {
      offset = offsetSeconds
    }
  }

  @Test
  fun nowSeconds_isTheDeviceClockBeforeAnyMeasurement() {
    val clock = ClockSync(Store(), deviceNowSeconds = { 1_000L })

    assertEquals(1_000L, clock.nowSeconds())
    assertEquals(null, clock.skewSeconds.value)
  }

  @Test
  fun observeServerTime_storesTheOffsetAndReportsTheSkew() {
    val store = Store()
    val clock = ClockSync(store, deviceNowSeconds = { 1_000L })

    val corrected = clock.observeServerTime(1_600L)

    assertEquals(600L, store.offset)
    assertEquals(1_600L, corrected)
    // Device is behind the server, so its skew is negative.
    assertEquals(-600L, clock.skewSeconds.value)
    assertEquals(1_600L, clock.nowSeconds())
  }

  @Test
  fun observeServerTime_reportsAPositiveSkewWhenTheDeviceIsAhead() {
    val clock = ClockSync(Store(), deviceNowSeconds = { 1_000L })

    clock.observeServerTime(400L)

    assertEquals(600L, clock.skewSeconds.value)
    assertEquals(400L, clock.nowSeconds())
  }
}
