package com.hatuka.swipeclean.data.review

import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaType
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Shared "change your decision" and folder-picker logic for the bin, moves and history screens. */
@Singleton
class DecisionActions @Inject constructor(
    private val decisions: DecisionRepository,
    private val media: MediaRepository,
    private val settings: SettingsRepository,
) {
    /** Existing folders an item of [type] may be moved to (plus the default target). */
    suspend fun folderOptions(type: MediaType, currentPath: String?): List<String> {
        val existing = media.loadBuckets(MediaFilter.BOTH).buckets.mapNotNull { it.relativePath }
        return TargetPathRules.pickerOptions(existing + settings.defaultTarget.first(), type, currentPath)
    }

    suspend fun change(id: Long, state: DecisionState, targetPath: String? = null, makeDefault: Boolean = false) {
        if (makeDefault && targetPath != null && TargetPathRules.isValidForAll(targetPath)) settings.setDefaultTarget(targetPath)
        decisions.changeDecision(listOf(id), state, if (state == DecisionState.MOVE_PENDING) targetPath else null)
    }
}
