package com.example.elisdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the release version is wired from the single source into BuildConfig. */
class BuildConfigVersionTest {

  @Test
  fun `BuildConfig exposes version code and name from configured source`() {
    assertEquals("1.9.0", BuildConfig.VERSION_NAME)
    assertEquals(16, BuildConfig.VERSION_CODE)
  }

  @Test
  fun `buildConfig has no baked-in server url fallback`() {
    assertTrue(BuildConfig.SERVER_BASE_URL != "http://10.0.2.2:8000")
  }
}