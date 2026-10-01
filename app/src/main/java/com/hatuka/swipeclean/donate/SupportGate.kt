package com.hatuka.swipeclean.donate

import com.hatuka.swipeclean.core.support.SupportPrompt
import com.hatuka.swipeclean.data.review.ProgressRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Decides, at a natural break, whether to open the one-time donation page now. */
@Singleton
class SupportGate @Inject constructor(
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
) {
    /** True (once) when the donation page should open; remembers that it did. */
    suspend fun takePrompt(): Boolean {
        val total = progress.current().totalReviewed
        if (!SupportPrompt.shouldShow(total, settings.supportPromptShown.first())) return false
        settings.setSupportPromptShown()
        return true
    }
}
