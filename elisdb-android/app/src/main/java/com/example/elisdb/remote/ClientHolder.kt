package com.example.elisdb.remote

/**
 * Shares a single client per settings signature and keeps it alive while any
 * caller still holds a lease. When the settings change, the old client is
 * retired and closed only after the last in-flight request releases it.
 */
class ClientHolder<T>(
  private val currentSignature: () -> String,
  private val factory: () -> T,
  private val close: (T) -> Unit,
) {
  internal class Entry<T>(val signature: String, val client: T) {
    var references: Int = 0
    var retired: Boolean = false
    var closed: Boolean = false
  }

  private val lock = Any()
  private var current: Entry<T>? = null

  inner class Lease internal constructor(private val entry: Entry<T>) {
    val client: T
      get() = entry.client

    private var released = false

    fun release() {
      synchronized(lock) {
        if (released) return
        released = true
        entry.references--
        closeIfUnused(entry)
      }
    }
  }

  /** Returns the client for the current settings, creating it when needed. */
  fun acquire(): Lease = synchronized(lock) {
    val signature = currentSignature()
    var entry = current
    if (entry == null || entry.signature != signature) {
      val replacement = Entry(signature, factory())
      val previous = entry
      current = replacement
      if (previous != null) {
        previous.retired = true
        closeIfUnused(previous)
      }
      entry = replacement
    }
    entry.references++
    Lease(entry)
  }

  /** Closes a retired client once every lease has been released. */
  private fun closeIfUnused(entry: Entry<T>) {
    if (!entry.retired || entry.references > 0 || entry.closed) return
    entry.closed = true
    if (current === entry) current = null
    close(entry.client)
  }
}
