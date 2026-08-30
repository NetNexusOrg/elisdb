package com.example.elisdb.sync

import java.security.MessageDigest

/** Content-based identity for photos: SHA-256 over the decoded bytes. */
object PhotoHashes {
  fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
      .digest(bytes)
      .joinToString("") { "%02x".format(it) }
}