package com.hatuka.swipeclean.ui.common

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.hatuka.swipeclean.R
import java.text.NumberFormat

/** Localized short size such as "1.2 GB" (uses the app's locale, including Hebrew). */
@Composable
fun formatSize(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

@Composable
fun itemsCount(count: Int): String =
    pluralStringResource(R.plurals.items_count, count, NumberFormat.getIntegerInstance().format(count))

@Composable
fun countAndSize(count: Int, bytes: Long): String =
    stringResource(R.string.home_count_size, itemsCount(count), formatSize(bytes))
