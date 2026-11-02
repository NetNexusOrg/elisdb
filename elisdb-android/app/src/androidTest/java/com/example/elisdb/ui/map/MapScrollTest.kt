package com.example.elisdb.ui.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.example.elisdb.theme.ElisdbTheme
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.geometry.LatLng

class MapScrollTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun mapaNaoInterativoNaoEngoleScrollVertical() {
    composeTestRule.setContent {
      ElisdbTheme {
        Column(Modifier.verticalScroll(rememberScrollState())) {
          Box(Modifier.fillMaxWidth().height(700.dp)) {
            ElisMap(
              markerPosition = LatLng(38.72, -9.13),
              interactive = false,
              draggable = false,
            )
          }
          repeat(10) { index ->
            Text("Linha abaixo do mapa $index", Modifier.padding(vertical = 32.dp))
          }
        }
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Linha abaixo do mapa 8").assertIsNotDisplayed()

    repeat(3) {
      composeTestRule.onRoot().performTouchInput {
        swipe(
          start = Offset(center.x, height * 0.7f),
          end = Offset(center.x, height * 0.15f),
          durationMillis = 300,
        )
      }
      composeTestRule.waitForIdle()
    }

    composeTestRule.onNodeWithText("Linha abaixo do mapa 8").assertIsDisplayed()
  }
}
