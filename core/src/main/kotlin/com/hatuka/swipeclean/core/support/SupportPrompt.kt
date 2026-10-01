package com.hatuka.swipeclean.core.support

/**
 * The app has no ads; instead it asks once, gently, whether the user would like to donate. That
 * happens at a natural break (leaving the swipe screen, never in the middle of swiping) after the
 * user has reviewed at least [THRESHOLD] items in total, so they had time to enjoy the app.
 */
object SupportPrompt {
    const val THRESHOLD = 50

    fun shouldShow(totalReviewed: Int, alreadyShown: Boolean, threshold: Int = THRESHOLD): Boolean =
        !alreadyShown && totalReviewed >= threshold
}
