package com.example.elisdb.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keeps the device↔server clock offset current and exposes the measured skew.
 *
 * Records are stamped with "device clock + offset" so last-write-wins compares
 * them on the server's timescale even when the device clock is wrong. The
 * offset is re-measured from the `Date` header of every server response (no
 * extra requests) and can be corrected on demand from the settings screen.
 */
class ClockSync(
  private val store: ClockOffsetStore,
  private val deviceNowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) {
  private val _skewSeconds = MutableStateFlow<Long?>(null)

  /**
   * Device clock minus server clock, in seconds: negative when the device is
   * behind. Null until a server time has been observed.
   */
  val skewSeconds: StateFlow<Long?> = _skewSeconds.asStateFlow()

  /** Timestamp to sign requests with: the device clock shifted onto the server's. */
  fun nowSeconds(): Long = deviceNowSeconds() + (store.getOffsetSeconds() ?: 0L)

  /**
   * Persists the offset implied by [serverTime] and returns the corrected now.
   * Safe to call on every response: it only rewrites the stored offset.
   */
  fun observeServerTime(serverTime: Long): Long {
    val offset = serverTime - deviceNowSeconds()
    store.setOffsetSeconds(offset)
    _skewSeconds.value = -offset
    return nowSeconds()
  }
}
