package com.example.elisdb.ui.form

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.ClienteRepositoryImpl
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.theme.ElisdbTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateFormStartsEmptyTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun novo_cliente_formComecaVazio() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository =
      ClienteRepositoryImpl(context, database, database.clienteDao(), database.fotoDao())
    runBlocking {
      repository.save(ClienteDraft(nome = "Ana", latitude = 38.7, longitude = -9.1))
    }

    val primeiroViewModel = ClientFormViewModel(repository, { null }, null)
    val segundoViewModel = ClientFormViewModel(repository, { null }, null)
    var isSegundo by mutableStateOf(false)
    composeTestRule.setContent {
      ElisdbTheme {
        ClientFormScreen(
          viewModel = if (isSegundo) segundoViewModel else primeiroViewModel,
          clienteId = null,
          onSaved = {},
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Novo cliente").assertExists()
    val ativoTop =
      composeTestRule.onNodeWithText("Cliente ativo").fetchSemanticsNode().boundsInRoot.top
    val contratoTop =
      composeTestRule.onNodeWithText("Contrato").fetchSemanticsNode().boundsInRoot.top
    assertTrue("O toggle ativo deve ser o primeiro campo", ativoTop < contratoTop)
    composeTestRule.onNodeWithText("Tag").assertExists()
    composeTestRule.onNodeWithText("O cliente está em serviço.").assertDoesNotExist()
    composeTestRule
      .onNodeWithText("Cliente inativo: fica guardado, mas marcado como fora de serviço.")
      .assertDoesNotExist()
    composeTestRule.onNodeWithText("Ana").assertDoesNotExist()

    isSegundo = true
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Novo cliente").assertExists()
    composeTestRule.onNodeWithText("Ana").assertDoesNotExist()
    composeTestRule.onNodeWithText("Guardar").assertExists()
    database.close()
  }
}
