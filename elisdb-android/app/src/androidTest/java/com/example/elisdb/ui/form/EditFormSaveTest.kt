package com.example.elisdb.ui.form

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepositoryImpl
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.theme.ElisdbTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditFormSaveTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun editar_cliente_guardaAlteracoesNaBaseDeDados() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository =
      ClienteRepositoryImpl(context, database, database.clienteDao(), database.fotoDao())
    val id =
      runBlocking {
        repository.save(ClienteDraft(nome = "Smoke Test", latitude = 38.7, longitude = -9.1))
      }

    var saved = false
    val viewModel = ClientFormViewModel(repository, { null }, id)
    composeTestRule.setContent {
      ElisdbTheme {
        ClientFormScreen(
          viewModel = viewModel,
          clienteId = id,
          onSaved = { saved = true },
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Editar cliente").assertExists()
    composeTestRule.onNodeWithText("SMOKE TEST").assertExists()

    composeTestRule.onNodeWithText("SMOKE TEST").performTextReplacement("Cliente Editado")
    composeTestRule.onNodeWithText("Guardar").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertTrue(saved)
    val updated = runBlocking { repository.getCliente(id) }
    assertEquals("CLIENTE EDITADO", updated?.cliente?.nome)

    database.close()
  }
}
