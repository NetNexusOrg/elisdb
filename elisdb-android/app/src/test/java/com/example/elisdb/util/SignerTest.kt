package com.example.elisdb.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SignerTest {

  private val seedB64 = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
  private val signer = Signer.fromBase64(seedB64)
  private val body = "{\"records\":[]}".toByteArray()
  private val timestamp = 1700000000L

  private val referenceDigest = "1b8b4c0b6f6ad1d32565952720bc004eeb1f188f62045e4d5525ae2af8c78432"
  private val referenceSignature = "9ceQhOcbWzPlaasFsE3jtgotXBFl5xvPG5sb9j6LFpAh5VTpWK9MfsJ5yQ9jPGgnoU/5Lkx5HR6sh5HAAU6/BA=="

  @Test
  fun sha256_matchesReference() {
    assertEquals(referenceDigest, signer.sha256(body))
  }

  @Test
  fun buildMessage_matchesContract() {
    val message = signer.buildMessage("POST", "/api/push", body, timestamp)
    assertEquals(
      "POST\n/api/push\n" + referenceDigest + "\n1700000000",
      message.toString(Charsets.UTF_8),
    )
  }

  @Test
  fun signature_matchesReferenceVector() {
    val signature = signer.signRequest("POST", "/api/push", body, timestamp)
    assertEquals(referenceSignature, signature)
  }
}
