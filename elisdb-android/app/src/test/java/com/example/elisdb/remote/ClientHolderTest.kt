package com.example.elisdb.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientHolderTest {
  private class FakeClient(val id: Int) {
    var closed = false
    fun close() {
      closed = true
    }
  }

  @Test
  fun `requests with the same settings share one client`() {
    var signature = "a"
    var created = 0
    val holder = ClientHolder({ signature }, { FakeClient(++created) }, { it.close() })

    val first = holder.acquire()
    val second = holder.acquire()

    assertSame(first.client, second.client)
    assertEquals(1, created)
    first.release()
    second.release()
    assertFalse(first.client.closed)
  }

  @Test
  fun `a replaced client closes only after its last lease is released`() {
    var signature = "a"
    var created = 0
    val holder = ClientHolder({ signature }, { FakeClient(++created) }, { it.close() })
    val old = holder.acquire()

    signature = "b"
    val new = holder.acquire()

    assertNotSame(old.client, new.client)
    assertFalse("old client still serving its request", old.client.closed)

    old.release()
    assertTrue("old client closes after the request", old.client.closed)
    assertFalse("new client stays open", new.client.closed)
    new.release()
  }

  @Test
  fun `a late lease never receives a retired client after a swap`() {
    var signature = "a"
    var created = 0
    val holder = ClientHolder({ signature }, { FakeClient(++created) }, { it.close() })
    val old = holder.acquire()
    old.release()
    signature = "b"

    val new = holder.acquire()

    assertSame("new requests use the new settings", 2, new.client.id)
    old.release() // idempotent release must not close the new client
    assertFalse(new.client.closed)
    new.release()
  }
}
