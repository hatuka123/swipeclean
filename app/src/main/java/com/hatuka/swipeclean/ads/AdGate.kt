package com.hatuka.swipeclean.ads

import android.app.Activity
import com.hatuka.swipeclean.core.ads.AdPolicy
import com.hatuka.swipeclean.data.review.ProgressRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Shows a full-screen ad; returns true if one was actually shown. */
interface AdPresenter {
    suspend fun show(activity: Activity): Boolean
}

/**
 * No ads yet: a real ad SDK needs the INTERNET permission, which the app does not have (and CI
 * forbids). Replace this binding once the ad account and the policy change are approved.
 */
class NoAdPresenter @Inject constructor() : AdPresenter {
    override suspend fun show(activity: Activity) = false
}

/** Called at a natural break (leaving the swipe screen); shows the ad when [AdPolicy] allows it. */
@Singleton
class AdGate @Inject constructor(
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val presenter: AdPresenter,
) {
    suspend fun onNaturalBreak(activity: Activity) {
        val total = progress.current().totalReviewed
        if (!AdPolicy.shouldShow(total, settings.adReviewedMark.first())) return
        if (presenter.show(activity)) settings.setAdReviewedMark(total)
    }
}
