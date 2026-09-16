package com.example.elisdb.ui.form

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.elisdb.data.ClienteRepository
import com.example.elisdb.sync.SyncStatus
import com.example.elisdb.data.ClienteDraft
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Foto
import com.example.elisdb.theme.ElisdbTheme
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class MapPickerFormTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun form_tocarPreviaAbreOmapa_eCancelarNaoAltera() {
    val viewModel = ClientFormViewModel(FakeFormRepository(), { null }, null)
    composeTestRule.setContent {
      ElisdbTheme {
        ClientFormScreen(viewModel = viewModel, clienteId = null, onSaved = {}, onBack = {})
      }
    }

    composeTestRule.onNodeWithText("Novo cliente").assertExists()
    composeTestRule.onNodeWithText("Localização").assertExists()
    composeTestRule.onNodeWithText("Escolher localização").assertDoesNotExist()

    composeTestRule.onNodeWithTag("map_preview").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Escolher localização").assertExists()

    composeTestRule.onNodeWithContentDescription("Cancelar").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Escolher localização").assertDoesNotExist()
    composeTestRule.onNodeWithText("Novo cliente").assertExists()
  }
}

private class FakeFormRepository : ClienteRepository {
  override val clientes: Flow<List<Cliente>> = flowOf(emptyList())

  override val clientesComFotos: Flow<List<ClienteComFotos>> = flowOf(emptyList())

  override fun observeCliente(id: String): Flow<ClienteComFotos?> = flowOf(null)

  override suspend fun getCliente(id: String): ClienteComFotos? = null

  override suspend fun stagePhoto(source: Uri): Foto = error("Not used in this test")

  override fun createPhotoFile(): File = error("Not used in this test")

  override suspend fun deletePhotoFile(foto: Foto) = Unit

  override suspend fun save(draft: ClienteDraft): String = error("Not used in this test")

  override suspend fun deleteCliente(id: String) = Unit

  override val syncStatus = flowOf(SyncStatus.IDLE)

  override val syncFault: Flow<com.example.elisdb.sync.SyncFault?> = flowOf(null)

  override suspend fun syncNow(): SyncStatus = SyncStatus.IDLE
}
