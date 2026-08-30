package com.example.elisdb.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ThemeModeTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var uiModeManager: UiModeManager
  private var originalMode = UiModeManager.MODE_NIGHT_AUTO

  @Before
  fun setUp() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    uiModeManager =
      composeTestRule.activity.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    originalMode = uiModeManager.nightMode
  }

  @After
  fun tearDown() {
    uiModeManager.setApplicationNightMode(originalMode)
  }

  @Test
  fun temaSistemaEscuro_usaEsquemaEscuro() {
    setNightModeAndWait(UiModeManager.MODE_NIGHT_YES)
    val scheme = captureThemeColorScheme()
    assertTrue(
      "Esperado background escuro, obtido ${scheme.background}",
      scheme.background.luminance() < 0.5,
    )
  }

  @Test
  fun temaSistemaClaro_usaEsquemaClaro() {
    setNightModeAndWait(UiModeManager.MODE_NIGHT_NO)
    val scheme = captureThemeColorScheme()
    assertTrue(
      "Esperado background claro, obtido ${scheme.background}",
      scheme.background.luminance() > 0.5,
    )
  }

  private fun setNightModeAndWait(mode: Int) {
    composeTestRule.activity.runOnUiThread { uiModeManager.setApplicationNightMode(mode) }
    val target =
      if (mode == UiModeManager.MODE_NIGHT_YES) {
        Configuration.UI_MODE_NIGHT_YES
      } else {
        Configuration.UI_MODE_NIGHT_NO
      }
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      val current =
        composeTestRule.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
      current == target
    }
  }

  private fun captureThemeColorScheme(): ColorScheme {
    var scheme: ColorScheme? = null
    composeTestRule.setContent {
      ElisdbTheme {
        scheme = MaterialTheme.colorScheme
      }
    }
    composeTestRule.waitForIdle()
    return checkNotNull(scheme) { "ElisdbTheme não compôs um ColorScheme" }
  }
}
