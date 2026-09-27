package com.hatuka.swipeclean.core.ads

/**
 * When the (future) full-screen ad may appear. Only at a natural break (the user leaves the swipe
 * screen; never in the middle of swiping, per Play's disruptive-ads policy), and only after the
 * user reviewed at least [THRESHOLD] items since the last ad (or since installing the app).
 *
 * [totalReviewed] is the all-time reviewed count from the stats, so an undone swipe does not count.
 * [reviewedAtLastAd] is that count when the last ad was shown (0 before the first one).
 */
object AdPolicy {
    const val THRESHOLD = 50

    fun shouldShow(totalReviewed: Int, reviewedAtLastAd: Int, threshold: Int = THRESHOLD): Boolean =
        totalReviewed - reviewedAtLastAd >= threshold
}
