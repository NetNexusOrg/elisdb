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
  fun publicKey_matchesRfc8032Vector() {
    // RFC 8032, section 7.1: seed and public key for the first Ed25519 test vector.
    val vectorSigner =
      Signer(hexToBytes("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60"))
    assertEquals("11qYAYKxCrfVS/7TyWQHOg7hcvPapiMlrwIaaPcHURo=", vectorSigner.publicKeyB64)
  }

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

  private fun hexToBytes(hex: String): ByteArray =
    hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
