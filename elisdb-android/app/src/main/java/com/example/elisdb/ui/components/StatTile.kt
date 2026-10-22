package com.example.elisdb.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A compact number with its label, used to show comparable counts side by side. */
@Composable
fun StatTile(
  value: String,
  label: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
  Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Spacer(Modifier.height(8.dp))
    }
    Text(
      value,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
      color = valueColor,
      maxLines = 1,
    )
    Text(
      label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
