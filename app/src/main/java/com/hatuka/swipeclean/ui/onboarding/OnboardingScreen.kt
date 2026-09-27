package com.hatuka.swipeclean.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.ui.common.ForceLtr
import com.hatuka.swipeclean.ui.theme.LocalActionColors

/**
 * Explains the swipe gestures and why each permission is needed.
 * [denied] switches to the "no access" state with a shortcut to the app settings.
 */
@Composable
fun OnboardingScreen(
    denied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalActionColors.current
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.onboarding_body), style = MaterialTheme.typography.bodyLarge)

            // Gesture icons are physical directions (right = keep) in every language, so they
            // are not auto-mirrored in RTL.
            GestureRow(Icons.AutoMirrored.Filled.ArrowForward, actions.keep, stringResource(R.string.gesture_keep), mirror = true)
            GestureRow(Icons.AutoMirrored.Filled.ArrowBack, actions.delete, stringResource(R.string.gesture_delete), mirror = true)
            GestureRow(Icons.Filled.ArrowUpward, actions.move, stringResource(R.string.gesture_move), mirror = false)

            Spacer(Modifier.height(4.dp))
            if (denied) {
                InfoCard(Icons.Filled.PhotoLibrary, stringResource(R.string.denied_title), stringResource(R.string.denied_body), highlight = true)
            } else {
                InfoCard(Icons.Filled.PhotoLibrary, stringResource(R.string.perm_media_title), stringResource(R.string.perm_media_body))
            }
            InfoCard(Icons.Filled.Shield, stringResource(R.string.perm_safe_title), stringResource(R.string.perm_safe_body))
            InfoCard(Icons.Filled.NotificationsNone, stringResource(R.string.perm_notify_title), stringResource(R.string.perm_notify_body))

            Spacer(Modifier.height(4.dp))
            if (denied) {
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.denied_open_settings))
                }
                OutlinedButton(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.denied_try_again))
                }
            } else {
                Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_allow))
                }
            }
        }
    }
}

@Composable
private fun GestureRow(icon: ImageVector, color: Color, label: String, mirror: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(40.dp)) {}
            PhysicalIcon(icon, mirror)
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

/** Auto-mirrored icons flip in RTL; wrapping them in an LTR scope keeps the physical direction. */
@Composable
private fun PhysicalIcon(icon: ImageVector, mirror: Boolean) {
    if (mirror) {
        ForceLtr {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
    } else {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String, highlight: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
            }
        }
    }
}
