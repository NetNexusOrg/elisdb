package com.example.elisdb.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlPolicyTest {

  @Test
  fun `https is allowed for any host`() {
    assertTrue(isAllowedServerUrl("https://db.example.com"))
    assertTrue(isAllowedServerUrl("https://192.168.1.10:8443"))
    assertTrue(isAllowedServerUrl("https://[2001:db8::1]"))
  }

  @Test
  fun `public cleartext http is rejected`() {
    assertFalse(isAllowedServerUrl("http://db.example.com"))
    assertFalse(isAllowedServerUrl("http://8.8.8.8"))
    assertNotNull(validateServerUrl("http://db.example.com"))
  }

  @Test
  fun `loopback and localhost are allowed over http`() {
    assertTrue(isAllowedServerUrl("http://localhost:8000"))
    assertTrue(isAllowedServerUrl("http://localhost"))
    assertTrue(isAllowedServerUrl("http://127.0.0.1:8000"))
    assertTrue(isAllowedServerUrl("http://[::1]:8000"))
  }

  @Test
  fun `private ipv4 ranges are allowed over http`() {
    assertTrue(isAllowedServerUrl("http://10.0.0.5"))
    assertTrue(isAllowedServerUrl("http://172.16.0.1"))
    assertTrue(isAllowedServerUrl("http://172.31.255.254"))
    assertTrue(isAllowedServerUrl("http://192.168.1.10"))
    assertTrue(isAllowedServerUrl("http://169.254.1.1"))
    assertTrue(isAllowedServerUrl("http://100.64.0.1"))
    assertTrue(isAllowedServerUrl("http://100.127.255.254"))
  }

  @Test
  fun `public ipv4 neighbours of private ranges are rejected`() {
    assertFalse(isAllowedServerUrl("http://172.15.0.1"))
    assertFalse(isAllowedServerUrl("http://172.32.0.1"))
    assertFalse(isAllowedServerUrl("http://100.63.255.255"))
    assertFalse(isAllowedServerUrl("http://100.128.0.1"))
    assertFalse(isAllowedServerUrl("http://192.169.1.1"))
  }

  @Test
  fun `private ipv6 ranges are allowed over http`() {
    assertTrue(isAllowedServerUrl("http://[fc00::1]"))
    assertTrue(isAllowedServerUrl("http://[fd12:3456::1]"))
    assertTrue(isAllowedServerUrl("http://[fe80::1]"))
  }

  @Test
  fun `public ipv6 is rejected over http`() {
    assertFalse(isAllowedServerUrl("http://[2001:db8::1]"))
  }

  @Test
  fun `unsupported schemes and malformed urls are rejected`() {
    assertFalse(isAllowedServerUrl("ftp://db.example.com"))
    assertFalse(isAllowedServerUrl("db.example.com"))
    assertFalse(isAllowedServerUrl("http://"))
    assertNull(validateServerUrl("https://db.example.com"))
  }
}
