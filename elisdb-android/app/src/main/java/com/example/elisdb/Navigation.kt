package com.example.elisdb

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.elisdb.BuildConfig
import com.example.elisdb.ui.about.AboutScreen
import com.example.elisdb.ui.about.UpdateViewModel
import java.io.File
import com.example.elisdb.ui.cliente.ClientListScreen
import com.example.elisdb.ui.cliente.ClientListViewModel
import com.example.elisdb.ui.detalhe.ClientDetailScreen
import com.example.elisdb.ui.detalhe.ClientDetailViewModel
import com.example.elisdb.ui.detalhe.DocumentoViewerScreen
import com.example.elisdb.ui.detalhe.DocumentoViewerViewModel
import com.example.elisdb.ui.detalhe.ClientLogScreen
import com.example.elisdb.ui.detalhe.ClientLogViewModel
import com.example.elisdb.ui.detalhe.MapFullscreenScreen
import com.example.elisdb.ui.detalhe.PhotoViewerScreen
import com.example.elisdb.ui.detalhe.PhotoViewerViewModel
import com.example.elisdb.ui.form.ClientFormScreen
import com.example.elisdb.ui.form.ClientFormViewModel
import com.example.elisdb.ui.settings.ConfigScreen
import com.example.elisdb.ui.settings.ServerConfigViewModel
import com.example.elisdb.ui.settings.SyncViewModel
import com.example.elisdb.ui.rememberAppContainer

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(ClientList)
  val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
  val saveableStateHolderNavEntryDecorator =
    rememberSaveableStateHolderNavEntryDecorator<NavKey>(rememberSaveableStateHolder())
  val viewModelNavEntryDecorator =
    rememberViewModelStoreNavEntryDecorator<NavKey>(viewModelStoreOwner = viewModelStoreOwner)

  NavDisplay(
    backStack = backStack,
    entryDecorators =
      listOf(saveableStateHolderNavEntryDecorator, viewModelNavEntryDecorator),
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<ClientList> {
          val container = rememberAppContainer()
          ClientListScreen(
            viewModel = viewModel { ClientListViewModel(container.repository) },
            onClienteClick = { id -> backStack.add(ClientDetail(id)) },
            onAddClick = { backStack.add(ClientForm()) },
            onConfigClick = { backStack.add(Config) },
            onAboutClick = { backStack.add(About) },
          )
        }
        entry<Config> {
          val container = rememberAppContainer()
          val syncViewModel =
            viewModel {
              SyncViewModel(
                syncManager = container.syncManager,
                clienteCount = container.database.clienteDao().observeCount(),
                clockSkewSeconds = container.clockSync.skewSeconds,
                onCorrectClock = { container.refreshClock() },
              )
            }
          val serverViewModel =
            viewModel {
              ServerConfigViewModel(
                settingsStore = container.settingsStore,
                syncManager = container.syncManager,
                fetchKeyIdentity = {
                  try {
                    container.createApiClient().fetchKeyIdentity(timestamp = container.normalizedEpochSeconds())
                  } catch (e: Exception) {
                    null
                  }
                },
                resetSyncState = { container.resetSyncState() },
              )
            }
          ConfigScreen(
            syncViewModel = syncViewModel,
            serverViewModel = serverViewModel,
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<About> {
          val container = rememberAppContainer()
          val context = LocalContext.current
          val updateViewModel =
            viewModel {
              UpdateViewModel(
                installedVersionCode = BuildConfig.VERSION_CODE,
                isConfigured = { container.settingsStore.settings().signingKey.isNotBlank() },
                fetchRelease = {
                  container.createApiClient().fetchLatestRelease(container.normalizedEpochSeconds())
                },
                downloadRelease = { release, onProgress ->
                  container.createApiClient().downloadRelease(
                    release = release,
                    targetDir = File(context.cacheDir, "updates"),
                    timestamp = container.normalizedEpochSeconds(),
                    onProgress = onProgress,
                  )
                },
              )
            }
          AboutScreen(
            onBack = { backStack.removeLastOrNull() },
            updateViewModel = updateViewModel,
          )
        }
        entry<ClientForm> { key ->
          val container = rememberAppContainer()
          ClientFormScreen(
            viewModel =
              viewModel {
                ClientFormViewModel(
                  container.repository,
                  container.locationService::getCurrentLocation,
                  key.clienteId,
                )
              },
            clienteId = key.clienteId,
            onSaved = { backStack.removeLastOrNull() },
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<ClientDetail> { key ->
          val container = rememberAppContainer()
          ClientDetailScreen(
            viewModel = viewModel { ClientDetailViewModel(container.repository, key.clienteId) },
            onBack = { backStack.removeLastOrNull() },
            onEdit = { id -> backStack.add(ClientForm(id)) },
            onViewLog = { id -> backStack.add(ClientLog(id)) },
            onDeleted = { backStack.removeLastOrNull() },
            onPhotoClick = { index -> backStack.add(PhotoViewer(key.clienteId, index)) },
            onOpenDocumento = { documento -> backStack.add(DocumentoViewer(key.clienteId, documento.id)) },
            onOpenFullscreenMap = { id -> backStack.add(MapFullscreen(id)) },
          )
        }
        entry<ClientLog> { key ->
          val container = rememberAppContainer()
          ClientLogScreen(
            viewModel =
              viewModel {
                ClientLogViewModel(
                  repository = container.repository,
                  clienteId = key.clienteId,
                  fetchHistory = { id ->
                    container.createApiClient().fetchClientHistory(id, timestamp = container.normalizedEpochSeconds())
                  },
                )
              },
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<PhotoViewer> { key ->
          val container = rememberAppContainer()
          PhotoViewerScreen(
            viewModel =
              viewModel {
                PhotoViewerViewModel(container.repository, key.clienteId, key.indiceFoto)
              },
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<DocumentoViewer> { key ->
          val container = rememberAppContainer()
          DocumentoViewerScreen(
            viewModel =
              viewModel {
                DocumentoViewerViewModel(container.repository, key.clienteId, key.documentoId)
              },
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<MapFullscreen> { key ->
          val container = rememberAppContainer()
          MapFullscreenScreen(
            repository = container.repository,
            clienteId = key.clienteId,
            onBack = { backStack.removeLastOrNull() },
          )
        }
      },
  )
}
