package com.hatuka.swipeclean.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Swipe directions are physical (right = keep) in every language. Anything that encodes a
 * direction — gesture hints, the action-button row — is laid out LTR even in Hebrew.
 */
@Composable
fun ForceLtr(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}
