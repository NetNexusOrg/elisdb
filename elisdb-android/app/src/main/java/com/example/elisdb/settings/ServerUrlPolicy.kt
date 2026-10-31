package com.example.elisdb.settings

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.util.Locale

/**
 * Whether [url] may be used as the app's server address.
 *
 * HTTPS is allowed for any valid host. Cleartext HTTP is only allowed on a
 * local network: `localhost`, loopback, private IPv4 ranges (10/8, 172.16/12,
 * 192.168/16, 169.254/16, 100.64/10) and IPv6 loopback/ULA/link-local. A
 * public HTTP host is rejected so credentials and client data are not sent in
 * the clear over the internet.
 */
fun isAllowedServerUrl(url: String): Boolean = validateServerUrl(url) == null

/**
 * Validates [url] and returns a user-facing Portuguese error message, or null
 * when the address is acceptable (see [isAllowedServerUrl]).
 */
fun validateServerUrl(url: String): String? {
  val uri =
    try {
      URI(url.trim())
    } catch (_: Exception) {
      return "O endereço do servidor não é válido"
    }
  val scheme = uri.scheme?.lowercase(Locale.ROOT)
  val host = uri.host?.trim()
  if (host.isNullOrBlank()) {
    return "O endereço do servidor não é válido"
  }
  return when (scheme) {
    "https" -> null
    "http" ->
      if (isPrivateHost(host)) {
        null
      } else {
        "O servidor usa HTTP público, o que não é permitido; usa https:// ou um endereço da rede local"
      }
    else -> "O endereço do servidor tem de começar por http:// ou https://"
  }
}

/** True for localhost, loopback and the private/ULA/link-local IP ranges. */
private fun isPrivateHost(host: String): Boolean {
  val normalized = host.removeSurrounding("[", "]").lowercase(Locale.ROOT)
  if (normalized == "localhost" || normalized.endsWith(".localhost")) return true
  parseIpv4(normalized)?.let { return isPrivateIpv4(it) }
  if (!normalized.contains(':')) return false
  val address =
    try {
      InetAddress.getByName(normalized)
    } catch (_: Exception) {
      return false
    }
  return when (address) {
    is Inet4Address -> {
      val ipv4 = address.hostAddress?.let(::parseIpv4)
      ipv4 != null && isPrivateIpv4(ipv4)
    }
    is Inet6Address -> isPrivateIpv6(address.address)
    else -> false
  }
}

/** Parses a dotted-quad IPv4 literal into its 32-bit value, or null. */
private fun parseIpv4(host: String): Int? {
  val parts = host.split('.')
  if (parts.size != 4) return null
  var value = 0
  for (part in parts) {
    if (part.isEmpty() || part.length > 3 || !part.all { it.isDigit() }) return null
    val octet = part.toInt()
    if (octet > 255) return null
    value = (value shl 8) or octet
  }
  return value
}

private fun isPrivateIpv4(value: Int): Boolean {
  val firstOctet = value ushr 24
  val firstTwo = value ushr 16
  val firstTwelveBits = value ushr 20
  val firstTenBits = value ushr 22
  return firstOctet == 127 || // 127.0.0.0/8 loopback
    firstOctet == 10 || // 10.0.0.0/8
    firstTwelveBits == 0xAC1 || // 172.16.0.0/12
    firstTwo == 0xC0A8 || // 192.168.0.0/16
    firstTwo == 0xA9FE || // 169.254.0.0/16 link-local
    firstTenBits == 0x191 // 100.64.0.0/10 carrier-grade NAT
}

private fun isPrivateIpv6(bytes: ByteArray): Boolean {
  if (bytes.size != 16) return false
  val first = bytes[0].toInt() and 0xff
  val second = bytes[1].toInt() and 0xff
  val loopback = bytes.take(15).all { it == 0.toByte() } && bytes[15] == 1.toByte()
  return loopback || // ::1
    (first and 0xfe) == 0xfc || // fc00::/7 unique local
    (first == 0xfe && (second and 0xc0) == 0x80) // fe80::/10 link-local
}
