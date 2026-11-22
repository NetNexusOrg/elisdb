package com.example.elisdb.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign

/**
 * Confirmation prompt shared by the app's decision dialogs.
 *
 * The dismiss action sits against the left edge of the content, aligned with the
 * title, and the confirming action sits against the right edge. Keeping both in
 * one row avoids the default AlertDialog placement, which groups both actions on
 * the right and leaves the dismiss button near the middle of the dialog.
 */
@Composable
fun DecisionDialog(
  title: String,
  text: String,
  confirmLabel: String,
  onConfirm: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  dismissLabel: String = "Cancelar",
  confirmColor: Color = MaterialTheme.colorScheme.primary,
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    title = {
      Text(
        title,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    text = { Text(text) },
    confirmButton = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        TextButton(
          onClick = onDismissRequest,
          modifier = Modifier.testTag("decision_dialog_dismiss"),
        ) {
          Text(dismissLabel)
        }
        TextButton(
          onClick = onConfirm,
          modifier = Modifier.testTag("decision_dialog_confirm"),
          colors = ButtonDefaults.textButtonColors(contentColor = confirmColor),
        ) {
          Text(confirmLabel)
        }
      }
    },
  )
}
