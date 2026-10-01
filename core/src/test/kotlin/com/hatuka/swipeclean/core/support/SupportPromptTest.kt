package com.hatuka.swipeclean.core.support

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportPromptTest {

    @Test
    fun `not before 50 reviews`() {
        assertFalse(SupportPrompt.shouldShow(totalReviewed = 0, alreadyShown = false))
        assertFalse(SupportPrompt.shouldShow(totalReviewed = 49, alreadyShown = false))
    }

    @Test
    fun `once the user reviewed 50 items`() {
        assertTrue(SupportPrompt.shouldShow(totalReviewed = 50, alreadyShown = false))
        assertTrue(SupportPrompt.shouldShow(totalReviewed = 900, alreadyShown = false))
    }

    @Test
    fun `only once`() {
        assertFalse(SupportPrompt.shouldShow(totalReviewed = 900, alreadyShown = true))
    }
}
