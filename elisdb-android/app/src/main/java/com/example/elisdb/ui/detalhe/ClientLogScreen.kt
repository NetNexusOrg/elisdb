package com.example.elisdb.ui.detalhe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elisdb.remote.AuditEntryDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated page for a client's edit history: the log used to live in a small
 * dialog, which made long histories awkward to read. Entries are grouped by
 * day, with one card per change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientLogScreen(
  viewModel: ClientLogViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Log do cliente")
            state.contrato?.takeIf { it.isNotBlank() }?.let { contrato ->
              Text(
                contrato,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
          }
        },
      )
    },
  ) { innerPadding ->
    when {
      state.isLoading ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      state.error -> LogErrorState(
        message = "Não foi possível carregar o histórico do cliente.",
        onRetry = viewModel::retry,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
      state.entries.isEmpty() -> LogEmptyState(
        message =
          if (state.hasPending) {
            "Este cliente tem alterações por sincronizar. O histórico aparece depois de sincronizar."
          } else {
            "Ainda não há alterações registadas no servidor para este cliente."
          },
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
      else -> LogTimeline(
        clienteNome = state.clienteNome,
        entries = state.entries,
        photoUris = state.photoUris,
        hasMore = state.hasMore,
        isLoadingMore = state.isLoadingMore,
        onLoadMore = viewModel::loadMore,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
    }
  }
}

@Composable
private fun LogTimeline(
  clienteNome: String?,
  entries: List<AuditEntryDto>,
  photoUris: Map<String, String>,
  hasMore: Boolean,
  isLoadingMore: Boolean,
  onLoadMore: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
  val days = remember(entries) { entries.groupBy { dayFormat.format(Date(it.ts * 1000)) } }
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    clienteNome?.let { nome ->
      item { Text(nome, style = MaterialTheme.typography.headlineSmall) }
    }
    item {
      Text(
        text = if (entries.size == 1) "1 alteração" else "${entries.size} alterações",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    days.forEach { (day, dayEntries) ->
      item(key = "dia-$day") {
        Text(
          text = day,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      items(dayEntries) { entry ->
        LogEntryCard(entry = entry, timeFormat = timeFormat, photoUris = photoUris)
      }
    }
    if (hasMore || isLoadingMore) {
      item(key = "load-more") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          OutlinedButton(onClick = onLoadMore, enabled = !isLoadingMore) {
            Text(if (isLoadingMore) "A carregar…" else "Carregar mais")
          }
        }
      }
    }
  }
}
