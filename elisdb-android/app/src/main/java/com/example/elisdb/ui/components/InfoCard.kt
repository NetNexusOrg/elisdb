package com.example.elisdb.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A titled card used to group related read-only information on the settings screens.
 * A card whose content is self-explanatory (e.g. a status summary) may omit the title.
 */
@Composable
fun InfoCard(
  title: String? = null,
  modifier: Modifier = Modifier,
  trailing: (@Composable () -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (title != null || trailing != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (title != null) {
            Text(
              title,
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.weight(1f),
            )
          } else {
            Spacer(Modifier.weight(1f))
          }
          trailing?.invoke()
        }
      }
      content()
    }
  }
}

/** A single label/value pair with the shared visual hierarchy: small label, larger value. */
@Composable
fun InfoRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
  onValueClick: (() -> Unit)? = null,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      label,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    val valueModifier =
      if (onValueClick != null) Modifier.fillMaxWidth().clickable(onClick = onValueClick)
      else Modifier.fillMaxWidth()
    Text(
      value,
      style = MaterialTheme.typography.bodyLarge,
      color = if (onValueClick != null) MaterialTheme.colorScheme.primary else valueColor,
      modifier = valueModifier,
    )
  }
}
