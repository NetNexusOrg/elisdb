package com.example.elisdb.remote

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpClientArchitectureTest {

  @Test
  fun `only ApiClient constructs an HTTP client in production code`() {
    val root =
      listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull { it.isDirectory }
        ?: error("cannot locate app/src/main/java from ${File(".").absolutePath}")
    val offenders =
      root.walkTopDown()
        .filter { it.isFile && it.extension == "kt" && it.name != "ApiClient.kt" }
        .filter { it.readText().contains("HttpClient(") }
        .map { it.relativeTo(root).path }
        .toList()

    assertTrue("HTTP clients must go through ApiClient: $offenders", offenders.isEmpty())
  }
}
