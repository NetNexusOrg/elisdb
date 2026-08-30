package com.example.elisdb.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.Security
import java.util.Base64
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec

/** Header names used by the ElisDB server auth. */
object AuthHeaders {
  const val TIMESTAMP = "X-Elisdb-Timestamp"
  const val SIGNATURE = "X-Elisdb-Signature"
  const val KEY = "X-Elisdb-Key"
}

/**
 * Signs API requests per the server-auth contract:
 * `METHOD\nPATH\nSHA256(body)\nTIMESTAMP` using the in-app ed25519 private key seed.
 */
class Signer(private val seed: ByteArray) {

  init {
    require(seed.size == 32) { "ed25519 seed must be 32 bytes" }
    // Register the EdDSA provider exactly once for the process. Constructing a
    // new Signer (e.g. on every sync) must not keep adding duplicate providers
    // to the static Security list.
    ensureProviderRegistered()
  }

  private val parameterSpec: EdDSAParameterSpec =
    EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)

  private val privateKey: EdDSAPrivateKey =
    EdDSAPrivateKey(EdDSAPrivateKeySpec(seed, parameterSpec))

  /** Base64 of the raw 32-byte ed25519 public key, sent in `X-Elisdb-Key`. */
  val publicKeyB64: String = Base64.getEncoder().encodeToString(privateKey.getAbyte())

  /** Returns the SHA256 hex digest of [body]. */
  fun sha256(body: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(body)
    return digest.joinToString("") { "%02x".format(it) }
  }

  /** Builds the message signed by the server: METHOD\nPATH\nSHA256(body)\nTIMESTAMP. */
  fun buildMessage(method: String, path: String, body: ByteArray, timestamp: Long): ByteArray =
    "${method}\n${path}\n${sha256(body)}\n${timestamp}".toByteArray(StandardCharsets.UTF_8)

  /** Produces the base64 ed25519 signature over [message] with the in-app key. */
  fun sign(message: ByteArray): String {
    val signature = java.security.Signature.getInstance("NONEwithEdDSA", EdDSASecurityProvider.PROVIDER_NAME)
    signature.initSign(privateKey)
    signature.update(message)
    return Base64.getEncoder().encodeToString(signature.sign())
  }

  /** Convenience: builds the request message and signs it. */
  fun signRequest(method: String, path: String, body: ByteArray, timestamp: Long): String =
    sign(buildMessage(method, path, body, timestamp))

  companion object {
    /** Decodes a base64, 32-byte ed25519 seed. */
    fun fromBase64(seedB64: String): Signer {
      val seed = Base64.getDecoder().decode(seedB64)
      return Signer(seed)
    }
  }
}

/** Registers the EdDSA security provider at most once for the process. */
private fun ensureProviderRegistered() {
  if (Security.getProvider(EdDSASecurityProvider.PROVIDER_NAME) == null) {
    Security.addProvider(EdDSASecurityProvider())
  }
}