package com.hatuka.swipeclean.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.move.TargetPathRules

const val FOLDER_PICKER_TAG = "folder_picker"
const val NEW_FOLDER_FIELD_TAG = "new_folder_field"

/**
 * Choose an existing folder or type a new name (created under Pictures/). Optionally makes the
 * choice the default for a plain swipe up (only for folders that accept photos and videos).
 * [options] null = still loading.
 */
@Composable
fun FolderPickerDialog(
    options: List<String>?,
    defaultTarget: String,
    onPick: (path: String, makeDefault: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var makeDefault by rememberSaveable { mutableStateOf(false) }
    val target = selected ?: TargetPathRules.newFolder(newName)
    val canBeDefault = target != null && TargetPathRules.isValidForAll(target) && !TargetPathRules.sameFolder(target, defaultTarget)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(FOLDER_PICKER_TAG),
        title = { Text(stringResource(R.string.picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (options == null) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(options, key = { it }) { path ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = path
                                        newName = ""
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected == path, onClick = null)
                                Column(Modifier.padding(start = 8.dp)) {
                                    val isDefault = TargetPathRules.sameFolder(path, defaultTarget)
                                    Text(
                                        TargetPathRules.displayName(path) + if (isDefault) " · " + stringResource(R.string.picker_default_badge) else "",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        if (it.isNotBlank()) selected = null
                    },
                    label = { Text(stringResource(R.string.picker_new_folder)) },
                    supportingText = { TargetPathRules.newFolder(newName)?.let { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag(NEW_FOLDER_FIELD_TAG),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canBeDefault) { makeDefault = !makeDefault },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = makeDefault && canBeDefault, onCheckedChange = null, enabled = canBeDefault)
                    Text(stringResource(R.string.picker_make_default), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { target?.let { onPick(it, makeDefault && canBeDefault) } }, enabled = target != null) {
                Text(stringResource(R.string.picker_move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
