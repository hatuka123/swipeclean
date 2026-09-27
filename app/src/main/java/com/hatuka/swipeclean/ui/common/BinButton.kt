package com.hatuka.swipeclean.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hatuka.swipeclean.R
import java.text.NumberFormat

/** Top-bar entry to the bin, with the number of items marked for deletion. */
@Composable
fun BinButton(count: Int, onOpenBin: () -> Unit) {
    IconButton(onClick = onOpenBin) {
        BadgedBox(badge = { if (count > 0) Badge { Text(NumberFormat.getIntegerInstance().format(count)) } }) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.open_bin))
        }
    }
}
