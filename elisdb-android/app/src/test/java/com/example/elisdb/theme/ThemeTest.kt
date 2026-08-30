package com.example.elisdb.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

  @Test
  fun darkMode_semCoresDinamicas_usaEsquemaEscuro() {
    val scheme = resolveColorScheme(darkTheme = true, dynamicColor = false, dynamicScheme = null)
    assertEquals(DarkColorScheme, scheme)
  }

  @Test
  fun lightMode_semCoresDinamicas_usaEsquemaClaro() {
    val scheme = resolveColorScheme(darkTheme = false, dynamicColor = false, dynamicScheme = null)
    assertEquals(LightColorScheme, scheme)
  }

  @Test
  fun dynamicColor_comProvider_usaOEsquemaDinamicoDoModo() {
    val darkDynamic = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF000001))
    val lightDynamic = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF000002))

    val darkScheme =
      resolveColorScheme(darkTheme = true, dynamicColor = true, dynamicScheme = { dark -> if (dark) darkDynamic else lightDynamic })
    val lightScheme =
      resolveColorScheme(darkTheme = false, dynamicColor = true, dynamicScheme = { dark -> if (dark) darkDynamic else lightDynamic })

    assertEquals(darkDynamic, darkScheme)
    assertEquals(lightDynamic, lightScheme)
  }

  @Test
  fun dynamicColor_semProvider_caiNoEsquemaEstatico() {
    val darkScheme = resolveColorScheme(darkTheme = true, dynamicColor = true, dynamicScheme = null)
    val lightScheme = resolveColorScheme(darkTheme = false, dynamicColor = true, dynamicScheme = null)

    assertEquals(DarkColorScheme, darkScheme)
    assertEquals(LightColorScheme, lightScheme)
  }

  @Test
  fun dynamicColor_falso_ignoraOProvider() {
    var chamado = false
    val provider: (Boolean) -> ColorScheme = {
      chamado = true
      lightColorScheme()
    }

    val scheme = resolveColorScheme(darkTheme = true, dynamicColor = false, dynamicScheme = provider)

    assertEquals(DarkColorScheme, scheme)
    assertEquals(false, chamado)
  }
}
