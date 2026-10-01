package com.hatuka.swipeclean.ui.swipe

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.hatuka.swipeclean.R

/** The parts of the swipe screen the tour points at. */
enum class TourTarget { CARD, KEEP, DELETE, MOVE, UNDO, BIN, SHARE }

data class TourStep(val target: TourTarget, @StringRes val text: Int)

/** The first-time explanation of the swipe screen, one small bubble per step. */
val TOUR_STEPS = listOf(
    TourStep(TourTarget.CARD, R.string.tour_card),
    TourStep(TourTarget.KEEP, R.string.tour_keep),
    TourStep(TourTarget.DELETE, R.string.tour_delete),
    TourStep(TourTarget.MOVE, R.string.tour_move),
    TourStep(TourTarget.UNDO, R.string.tour_undo),
    TourStep(TourTarget.BIN, R.string.tour_bin),
    TourStep(TourTarget.SHARE, R.string.tour_share),
)

/** Where each tour target is on screen (root coordinates), filled in by [tourAnchor]. */
@Stable
class TourAnchors {
    val bounds = mutableStateMapOf<TourTarget, Rect>()
}

fun Modifier.tourAnchor(anchors: TourAnchors, target: TourTarget): Modifier =
    onGloballyPositioned { anchors.bounds[target] = it.boundsInRoot() }

/**
 * Dims the screen except the current target and shows a bubble next to it with "next" and
 * "skip". Touches outside the bubble are blocked, so nothing is swiped by accident.
 */
@Composable
fun TourOverlay(
    anchors: TourAnchors,
    stepIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val step = TOUR_STEPS[stepIndex]
    val target = anchors.bounds[step.target]
    val last = stepIndex == TOUR_STEPS.lastIndex
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color.Black.copy(alpha = 0.62f))
            if (target != null) {
                val pad = 8.dp.toPx()
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(target.left - pad, target.top - pad),
                    size = Size(target.width + pad * 2, target.height + pad * 2),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    blendMode = BlendMode.Clear,
                )
            }
        }
        BubbleLayout(target) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(step.text), style = MaterialTheme.typography.bodyLarge)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            "${stepIndex + 1}/${TOUR_STEPS.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (!last) TextButton(onClick = onSkip) { Text(stringResource(R.string.tour_skip)) }
                        Spacer(Modifier.width(4.dp))
                        Button(onClick = onNext) {
                            Text(stringResource(if (last) R.string.tour_done else R.string.tour_next))
                        }
                    }
                }
            }
        }
    }
}

/** Places the bubble below the target, or above it when the target is in the lower half. */
@Composable
private fun BubbleLayout(target: Rect?, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val side = 16.dp.roundToPx()
        val gap = 14.dp.roundToPx()
        val width = (constraints.maxWidth - side * 2).coerceAtLeast(0)
        val bubble = measurables.first().measure(Constraints(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val maxY = constraints.maxHeight - bubble.height - side
            val y = when {
                target == null -> (constraints.maxHeight - bubble.height) / 2
                target.center.y > constraints.maxHeight / 2f -> target.top.toInt() - gap - bubble.height
                else -> target.bottom.toInt() + gap
            }.coerceIn(side, maxY.coerceAtLeast(side))
            bubble.place(side, y)
        }
    }
}
