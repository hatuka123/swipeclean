package com.hatuka.swipeclean.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {

    @Test
    fun `no ad before the first 50 reviews`() {
        assertFalse(AdPolicy.shouldShow(totalReviewed = 0, reviewedAtLastAd = 0))
        assertFalse(AdPolicy.shouldShow(totalReviewed = 49, reviewedAtLastAd = 0))
    }

    @Test
    fun `ad after 50 reviews`() {
        assertTrue(AdPolicy.shouldShow(totalReviewed = 50, reviewedAtLastAd = 0))
        assertTrue(AdPolicy.shouldShow(totalReviewed = 400, reviewedAtLastAd = 0))
    }

    @Test
    fun `counts only reviews since the last ad`() {
        assertFalse(AdPolicy.shouldShow(totalReviewed = 120, reviewedAtLastAd = 80))
        assertTrue(AdPolicy.shouldShow(totalReviewed = 130, reviewedAtLastAd = 80))
    }

    @Test
    fun `undone swipes lower the total and do not count`() {
        assertFalse(AdPolicy.shouldShow(totalReviewed = 45, reviewedAtLastAd = 50))
    }
}
