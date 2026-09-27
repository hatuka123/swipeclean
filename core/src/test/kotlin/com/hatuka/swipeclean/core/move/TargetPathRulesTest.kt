package com.hatuka.swipeclean.core.move

import com.hatuka.swipeclean.core.media.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetPathRulesTest {

    @Test
    fun `normalizes slashes and rejects traversal`() {
        assertEquals("Pictures/Found/", TargetPathRules.normalize(" Pictures//Found "))
        assertEquals("DCIM/Camera/", TargetPathRules.normalize("/DCIM/Camera/"))
        assertNull(TargetPathRules.normalize("Pictures/../Android"))
        assertNull(TargetPathRules.normalize("  / "))
    }

    @Test
    fun `allowed roots depend on media type`() {
        assertTrue(TargetPathRules.isValidFor(MediaType.IMAGE, "Pictures/Found/"))
        assertTrue(TargetPathRules.isValidFor(MediaType.IMAGE, "DCIM/Trip"))
        assertFalse(TargetPathRules.isValidFor(MediaType.IMAGE, "Movies/Clips/"))
        assertTrue(TargetPathRules.isValidFor(MediaType.VIDEO, "Movies/Clips/"))
        assertFalse(TargetPathRules.isValidFor(MediaType.VIDEO, "Download/"))
        assertFalse(TargetPathRules.isValidFor(MediaType.IMAGE, "Android/data/x/"))
        assertTrue(TargetPathRules.isValidForAll(TargetPathRules.DEFAULT_TARGET))
        assertFalse(TargetPathRules.isValidForAll("Movies/Clips/"))
    }

    @Test
    fun `new folders go under Pictures with a clean name`() {
        assertEquals("Pictures/Trip 2024/", TargetPathRules.newFolder("  Trip 2024 "))
        assertEquals("Pictures/ab/", TargetPathRules.newFolder("a/b"))
        assertEquals("Pictures/טיול/", TargetPathRules.newFolder("טיול"))
        assertNull(TargetPathRules.newFolder("  ..  "))
        assertNull(TargetPathRules.newFolder("///"))
        assertEquals(60, TargetPathRules.newFolder("x".repeat(100))!!.removePrefix("Pictures/").removeSuffix("/").length)
    }

    @Test
    fun `picker lists valid folders except the current one`() {
        val existing = listOf("DCIM/Camera/", "Pictures/Screenshots/", "Download/", "Movies/Clips/", "pictures/screenshots/")
        assertEquals(
            listOf("DCIM/Camera/", "Pictures/Screenshots/"),
            TargetPathRules.pickerOptions(existing, MediaType.IMAGE, currentPath = null),
        )
        assertEquals(
            listOf("Movies/Clips/", "Pictures/Screenshots/"),
            TargetPathRules.pickerOptions(existing, MediaType.VIDEO, currentPath = "DCIM/Camera/"),
        )
    }

    @Test
    fun `display names`() {
        assertEquals("Found", TargetPathRules.displayName("Pictures/Found/"))
        assertTrue(TargetPathRules.sameFolder("Pictures/Found", "pictures/found/"))
        assertFalse(TargetPathRules.sameFolder(null, "Pictures/Found/"))
    }
}
