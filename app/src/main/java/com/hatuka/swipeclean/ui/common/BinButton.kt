package com.hatuka.swipeclean.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hatuka.swipeclean.R
import java.text.NumberFormat

private const val MAX_SHOWN_COUNT = 9_999

/**
 * Top-bar entry to the bin, with the number of items marked for deletion. The number sits next to
 * the icon, inside the button: a corner badge grows outward and ran off the screen edge from three
 * digits on.
 */
@Composable
fun BinButton(count: Int, onOpenBin: () -> Unit) {
    val icon = @Composable { Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.open_bin)) }
    if (count <= 0) {
        IconButton(onClick = onOpenBin) { icon() }
        return
    }
    TextButton(onClick = onOpenBin, contentPadding = PaddingValues(horizontal = 12.dp)) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(
            binCountLabel(count),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
        )
    }
}

private fun binCountLabel(count: Int): String {
    val nf = NumberFormat.getIntegerInstance()
    return if (count > MAX_SHOWN_COUNT) nf.format(MAX_SHOWN_COUNT) + "+" else nf.format(count)
}
