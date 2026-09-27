package com.hatuka.swipeclean.ui.bin

import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.ui.common.formatSize
import com.hatuka.swipeclean.ui.theme.LocalActionColors
import java.util.Date

const val CHANGE_DECISION_TAG = "change_decision"

/**
 * Shows one reviewed item large and lets the user change their earlier decision (from the bin,
 * the moves list or the history). Whatever they choose, the item stays reviewed and never
 * returns to a swipe session by itself.
 */
@Composable
fun ChangeDecisionDialog(
    item: DecisionEntity,
    uri: Uri?,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalActionColors.current
    val date = remember(item.dateMillis) { DateFormat.getMediumDateFormat(context).format(Date(item.dateMillis)) }
    val state = item.state
    val status = when (state) {
        DecisionState.DELETE_PENDING -> stringResource(R.string.status_in_bin)
        DecisionState.MOVE_PENDING -> stringResource(R.string.status_move_pending, TargetPathRules.displayName(item.targetPath.orEmpty()))
        DecisionState.MOVED -> stringResource(R.string.status_moved, TargetPathRules.displayName(item.targetPath.orEmpty()))
        else -> stringResource(R.string.status_kept)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding()
                .testTag(CHANGE_DECISION_TAG),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (uri != null) {
                    AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.decision_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.item_info, date, formatSize(item.sizeBytes), item.bucketName ?: item.relativePath.orEmpty()),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(status, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (state != DecisionState.KEEP && state != DecisionState.MOVED) {
                        ActionButton(stringResource(R.string.decision_keep), colors.keep, onKeep, Modifier.weight(1f))
                    }
                    if (state != DecisionState.DELETE_PENDING) {
                        ActionButton(stringResource(R.string.decision_delete), colors.delete, onDelete, Modifier.weight(1f))
                    }
                    ActionButton(
                        stringResource(if (state == DecisionState.MOVE_PENDING) R.string.decision_other_folder else R.string.decision_move),
                        colors.move,
                        onMove,
                        Modifier.weight(1f),
                    )
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(if (state == DecisionState.DELETE_PENDING) R.string.decision_leave else R.string.decision_no_change),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        modifier = modifier,
    ) { Text(label, maxLines = 1) }
}
