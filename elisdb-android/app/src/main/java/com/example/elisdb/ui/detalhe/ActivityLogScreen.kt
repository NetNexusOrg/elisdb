package com.example.elisdb.ui.detalhe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * Global activity log: every recorded client change, newest first, regardless
 * of the client it belongs to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
  viewModel: ActivityLogViewModel,
  onBack: () -> Unit,
  onClienteClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Alterações") },
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
        message = "Não foi possível carregar as alterações.",
        onRetry = viewModel::retry,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
      state.entries.isEmpty() -> LogEmptyState(
        message = "Ainda não há alterações registadas.",
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
      else -> ActivityTimeline(
        entries = state.entries,
        photoUris = state.photoUris,
        existingClienteIds = state.existingClienteIds,
        hasMore = state.hasMore,
        isLoadingMore = state.isLoadingMore,
        onLoadMore = viewModel::loadMore,
        onClienteClick = onClienteClick,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
    }
  }
}

@Composable
private fun ActivityTimeline(
  entries: List<AuditEntryDto>,
  photoUris: Map<String, String>,
  existingClienteIds: Set<String>,
  hasMore: Boolean,
  isLoadingMore: Boolean,
  onLoadMore: () -> Unit,
  onClienteClick: (String) -> Unit,
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
        val clienteId = entry.clienteId
        LogEntryCard(
          entry = entry,
          timeFormat = timeFormat,
          photoUris = photoUris,
          clienteNome = entry.clienteNome ?: clienteId,
          onClienteClick =
            if (entry.action != "delete" && clienteId != null && clienteId in existingClienteIds) {
              { onClienteClick(clienteId) }
            } else {
              null
            },
        )
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
