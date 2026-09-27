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
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.ui.common.formatSize
import com.hatuka.swipeclean.ui.theme.LocalActionColors
import java.util.Date

const val CHANGE_DECISION_TAG = "change_decision"

/**
 * "Restore" from the bin: shows the item large and lets the user change their decision.
 * Choosing Keep replaces the deletion mark; the item stays reviewed and won't reappear in a
 * swipe session. (Moving to a folder joins these options in phase 3.)
 */
@Composable
fun ChangeDecisionDialog(
    item: DecisionEntity,
    uri: Uri?,
    onKeep: () -> Unit,
    onLeaveInBin: () -> Unit,
) {
    val context = LocalContext.current
    val date = remember(item.dateMillis) { DateFormat.getMediumDateFormat(context).format(Date(item.dateMillis)) }
    Dialog(onDismissRequest = onLeaveInBin, properties = DialogProperties(usePlatformDefaultWidth = false)) {
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
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.decision_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.item_info, date, formatSize(item.sizeBytes), item.bucketName ?: item.relativePath.orEmpty()),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onLeaveInBin, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.decision_leave), color = Color.White)
                    }
                    Button(
                        onClick = onKeep,
                        colors = ButtonDefaults.buttonColors(containerColor = LocalActionColors.current.keep, contentColor = Color.White),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.decision_keep))
                    }
                }
            }
        }
    }
}
