package com.example.elisdb.ui.form

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepositoryImpl
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.theme.ElisdbTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class EditFormLoadsSelectedClientOnlyTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun editar_cliente_carregaApenasOsDadosDoClienteSelecionado() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository =
      ClienteRepositoryImpl(context, database, database.clienteDao(), database.fotoDao())
    val idA =
      runBlocking {
        repository.save(ClienteDraft(nome = "Ana", latitude = 38.7, longitude = -9.1))
      }
    val idB =
      runBlocking {
        repository.save(ClienteDraft(nome = "Bruno", latitude = 38.8, longitude = -9.2))
      }

    val viewModelA = ClientFormViewModel(repository, { null }, idA)
    val viewModelB = ClientFormViewModel(repository, { null }, idB)
    var isB by mutableStateOf(false)
    composeTestRule.setContent {
      ElisdbTheme {
        ClientFormScreen(
          viewModel = if (isB) viewModelB else viewModelA,
          clienteId = if (isB) idB else idA,
          onSaved = {},
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Editar cliente").assertExists()
    composeTestRule.onNodeWithText("ANA").assertExists()
    composeTestRule.onNodeWithText("BRUNO").assertDoesNotExist()

    isB = true
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Editar cliente").assertExists()
    composeTestRule.onNodeWithText("BRUNO").assertExists()
    composeTestRule.onNodeWithText("ANA").assertDoesNotExist()

    database.close()
  }
}
