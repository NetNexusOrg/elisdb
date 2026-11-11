package com.example.elisdb

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.elisdb.settings.hasConfiguredUser
import com.example.elisdb.settings.hasCredentials
import com.example.elisdb.ui.about.AboutScreen
import com.example.elisdb.ui.about.AppUpdatePrompt
import com.example.elisdb.ui.about.rememberUpdateViewModel
import com.example.elisdb.ui.cliente.ClientListScreen
import com.example.elisdb.ui.cliente.ClientListViewModel
import com.example.elisdb.ui.cliente.ClientMapScreen
import com.example.elisdb.ui.cliente.ClientMapViewModel
import com.example.elisdb.ui.detalhe.ClientDetailScreen
import com.example.elisdb.ui.detalhe.ClientDetailViewModel
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
import com.example.elisdb.ui.startup.SetupRequiredScreen
import com.example.elisdb.ui.rememberAppContainer

@Composable
fun MainNavigation() {
  val container = rememberAppContainer()
  val settings by container.settings.collectAsStateWithLifecycle()
  var configOpen by rememberSaveable { mutableStateOf(false) }

  // While the configuration screen is open from the setup screen, system back
  // returns to the setup screen instead of closing the activity.
  BackHandler(enabled = configOpen) { configOpen = false }

  when {
    // Configuration keeps priority even after the credential is saved, so its
    // ViewModel survives long enough to run the first sync after the import.
    configOpen -> ConfigDestination(container = container, onBack = { configOpen = false })
    !settings.hasConfiguredUser() ->
      SetupRequiredScreen(
        missingUserName = settings.hasCredentials(),
        onOpenConfig = { configOpen = true },
      )
    else -> MainDestinations(container = container)
  }
}

@Composable
private fun MainDestinations(container: AppContainer) {
  val backStack = rememberNavBackStack(ClientList)
  val updateViewModel = rememberUpdateViewModel(container)
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
            viewModel =
              viewModel {
                ClientListViewModel(
                  repository = container.repository,
                  viewPreferences = container.clientViewPreferences,
                )
              },
            onClienteClick = { id -> backStack.add(ClientDetail(id)) },
            onAddClick = { backStack.add(ClientForm()) },
            onConfigClick = { backStack.add(Config) },
            onAboutClick = { backStack.add(About) },
            onMapClick = { backStack.add(ClientMap) },
          )
        }
        entry<ClientMap> {
          val container = rememberAppContainer()
          ClientMapScreen(
            viewModel =
              viewModel {
                ClientMapViewModel(
                  repository = container.repository,
                  locationProvider = container.locationService::getCurrentLocation,
                  viewPreferences = container.clientViewPreferences,
                )
              },
            onClienteClick = { id -> backStack.add(ClientDetail(id)) },
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<Config> {
          ConfigDestination(
            container = rememberAppContainer(),
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<About> {
          val aboutContainer = rememberAppContainer()
          AboutScreen(
            onBack = { backStack.removeLastOrNull() },
            updateViewModel = rememberUpdateViewModel(aboutContainer),
            serverAvailability = aboutContainer.serverAvailability.state,
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
                    container.withApiClient { api ->
                      api.fetchClientHistory(id, timestamp = container.normalizedEpochSeconds())
                    }
                  },
                  fetchHistoryPage = { id, limit, afterId ->
                    container.withApiClient { api ->
                      api.fetchClientHistoryPage(
                        id,
                        limit,
                        afterId,
                        timestamp = container.normalizedEpochSeconds(),
                      )
                    }
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
        entry<MapFullscreen> { key ->
          val container = rememberAppContainer()
          MapFullscreenScreen(
            repository = container.repository,
            clienteId = key.clienteId,
            onBack = { backStack.removeLastOrNull() },
            locationProvider = container.locationService::getCurrentLocation,
          )
        }
      },
  )

  AppUpdatePrompt(
    updateViewModel = updateViewModel,
    serverAvailability = container.serverAvailability.state,
    isAboutOpen = backStack.lastOrNull() is About,
    onUpdate = { backStack.add(About) },
  )
}

@Composable
private fun ConfigDestination(container: AppContainer, onBack: () -> Unit) {
  val syncViewModel =
    viewModel {
      SyncViewModel(
        syncManager = container.syncManager,
        clienteCount = container.database.clienteDao().observeCount(),
        clockSkewSeconds = container.clockSync.skewSeconds,
        onCorrectClock = { container.refreshClock() },
        serverAvailability = container.serverAvailability.state,
        onRefreshServer = { container.serverAvailability.refresh() },
      )
    }
  val serverViewModel =
    viewModel {
      ServerConfigViewModel(
        settingsStore = container.settingsStore,
        syncManager = container.syncManager,
        fetchKeyIdentity = {
          try {
            container.withApiClient { api ->
              api.fetchKeyIdentity(timestamp = container.normalizedEpochSeconds())
            }
          } catch (e: Exception) {
            null
          }
        },
        resetSyncState = { container.resetSyncState() },
        onSettingsChanged = { container.refreshSettings() },
      )
    }
  ConfigScreen(
    syncViewModel = syncViewModel,
    serverViewModel = serverViewModel,
    onBack = onBack,
  )
}
