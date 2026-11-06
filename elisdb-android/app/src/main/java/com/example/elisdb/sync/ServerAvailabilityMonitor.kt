package com.example.elisdb.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import com.example.elisdb.remote.AuthException
import com.example.elisdb.remote.ConfigException
import com.example.elisdb.remote.ServerException
import com.example.elisdb.settings.SettingsStore
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Owns the app-wide view of whether the configured server is reachable.
 *
 * A probe is intentionally cheap and unauthenticated (`/api/time`): its only
 * job is to distinguish "no server configured", "server reachable" and "no
 * transport-level response". Authentication and data errors remain sync
 * concerns and do not turn the server offline.
 */
class ServerAvailabilityMonitor(
  context: Context,
  private val settingsStore: SettingsStore,
  private val scope: CoroutineScope,
  private val probe: suspend () -> Unit,
  private val probeTimeoutMillis: Long = DEFAULT_PROBE_TIMEOUT_MILLIS,
) {
  private val appContext = context.applicationContext
  private val connectivity =
    runCatching {
      appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }.getOrNull()

  private val _state = MutableStateFlow(initialState())
  val state: StateFlow<ServerAvailability> = _state.asStateFlow()

  private val refreshMutex = Mutex()
  private val version = AtomicLong(0)

  private val networkCallback =
    object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        scope.launch { refresh() }
      }

      override fun onLost(network: Network) {
        // Do not show an old "online" state after the transport disappears.
        // A subsequent onAvailable/refresh restores it.
        if (isConfigured()) publish(ServerAvailability.Offline)
      }
    }

  init {
    runCatching { connectivity?.registerDefaultNetworkCallback(networkCallback) }
      .onFailure { Log.w(TAG, "Could not register network callback", it) }
    if (isConfigured()) {
      scope.launch { refresh() }
    }
  }

  /** Re-checks the server, deduplicating concurrent calls. */
  suspend fun refresh() = refreshMutex.withLock {
    val configState = configuredState()
    if (configState != null) {
      publish(configState)
      return@withLock
    }
    val runVersion = version.incrementAndGet()
    _state.value = ServerAvailability.Checking
    val result =
      try {
        withTimeout(probeTimeoutMillis) { probe() }
        ServerAvailability.Online
      } catch (e: TimeoutCancellationException) {
        ServerAvailability.Offline
      } catch (e: CancellationException) {
        throw e
      } catch (e: ConfigException) {
        Log.w(TAG, "Server probe rejected the stored configuration", e)
        ServerAvailability.ConfigError
      } catch (e: AuthException) {
        // The server answered; the credential is a sync concern.
        ServerAvailability.Online
      } catch (e: ServerException) {
        // An HTTP error still proves the server is reachable.
        ServerAvailability.Online
      } catch (e: Exception) {
        Log.w(TAG, "Server probe failed", e)
        ServerAvailability.Offline
      }
    if (version.get() == runVersion) _state.value = result
  }

  /** Called after settings changed; re-evaluates without waiting for a screen. */
  fun invalidate() {
    val configState = configuredState()
    if (configState != null) {
      publish(configState)
    } else {
      scope.launch { refresh() }
    }
  }

  /** Lets a completed sync update reachability without an extra request. */
  fun onSyncOutcome(outcome: SyncOutcome) {
    when (outcome) {
      SyncOutcome.Success,
      is SyncOutcome.SuccessWithStatuses,
      is SyncOutcome.SuccessReport,
      is SyncOutcome.DuplicateMerged -> publish(ServerAvailability.Online)
      SyncOutcome.AuthFailure,
      SyncOutcome.ClockFailure -> publish(ServerAvailability.Online)
      SyncOutcome.ConfigFault -> publish(ServerAvailability.ConfigError)
      // A transient sync failure does not tell us whether the server was
      // unreachable or merely returned 5xx. Probe to decide.
      SyncOutcome.TransientError -> scope.launch { refresh() }
    }
  }

  private fun initialState(): ServerAvailability =
    configuredState() ?: ServerAvailability.Checking

  private fun isConfigured(): Boolean = configuredState() == null

  /** Null when a URL and signing key are present; otherwise the visible state. */
  private fun configuredState(): ServerAvailability? =
    try {
      val settings = settingsStore.settings()
      if (settings.baseUrl.isBlank() || settings.signingKey.isBlank()) {
        ServerAvailability.NotConfigured
      } else {
        null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not read server settings", e)
      ServerAvailability.ConfigError
    }

  private fun publish(state: ServerAvailability) {
    version.incrementAndGet()
    _state.value = state
  }

  private companion object {
    const val TAG = "ElisdbAvailability"
    const val DEFAULT_PROBE_TIMEOUT_MILLIS = 5_000L
  }
}
