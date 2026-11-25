package com.example.elisdb.ui.form

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepositoryImpl
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.data.local.Foto
import com.example.elisdb.theme.ElisdbTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditFormChoosesProfilePhotoTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun tocarNaEstrelaMudaAFotoDePerfil() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository =
      ClienteRepositoryImpl(context, database, database.clienteDao(), database.fotoDao(), database.catalogoDao(), database.clienteServicoDao())
    val id =
      runBlocking {
        repository.save(
          ClienteDraft(
            nome = "Ana",
            fotos =
              listOf(
                Foto(id = "", clienteId = "", uriLocal = "/tmp/a.webp"),
                Foto(id = "", clienteId = "", uriLocal = "/tmp/b.webp", preferida = true),
              ),
          )
        )
      }

    val viewModel = ClientFormViewModel(repository, { null }, id)
    composeTestRule.setContent {
      ElisdbTheme { ClientFormScreen(viewModel = viewModel, clienteId = id, onSaved = {}, onBack = {}) }
    }
    composeTestRule.waitForIdle()

    val initiallyMarked =
      composeTestRule
        .onNodeWithTag("profile_photo_marked")
        .performScrollTo()
        .fetchSemanticsNode()
        .boundsInRoot
    val unmarked = composeTestRule.onAllNodesWithTag("profile_photo_unmarked")[0].performScrollTo()
    val unmarkedBounds = unmarked.fetchSemanticsNode().boundsInRoot
    assertTrue(
      "A estrela preenchida deve começar na segunda foto",
      initiallyMarked.left != unmarkedBounds.left,
    )

    unmarked.performClick()
    composeTestRule.waitForIdle()

    val markedNow =
      composeTestRule
        .onNodeWithTag("profile_photo_marked")
        .performScrollTo()
        .fetchSemanticsNode()
        .boundsInRoot
    assertEquals(unmarkedBounds.left, markedNow.left, 1f)
    assertEquals(
      1,
      composeTestRule.onAllNodesWithTag("profile_photo_marked").fetchSemanticsNodes().size,
    )
    assertEquals(
      1,
      composeTestRule.onAllNodesWithTag("profile_photo_unmarked").fetchSemanticsNodes().size,
    )
    database.close()
  }
}
