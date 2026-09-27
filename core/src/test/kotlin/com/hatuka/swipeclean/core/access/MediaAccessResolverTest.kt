package com.hatuka.swipeclean.core.access

import com.hatuka.swipeclean.core.access.MediaAccessResolver.READ_EXTERNAL_STORAGE
import com.hatuka.swipeclean.core.access.MediaAccessResolver.READ_MEDIA_IMAGES
import com.hatuka.swipeclean.core.access.MediaAccessResolver.READ_MEDIA_VIDEO
import com.hatuka.swipeclean.core.access.MediaAccessResolver.READ_MEDIA_VISUAL_USER_SELECTED
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaAccessResolverTest {

    private fun resolve(sdk: Int, vararg granted: String) =
        MediaAccessResolver.resolve(sdk) { it in granted }

    @Test
    fun `api 30 to 32 uses read external storage`() {
        assertEquals(MediaAccess.FULL, resolve(30, READ_EXTERNAL_STORAGE))
        assertEquals(MediaAccess.NONE, resolve(32))
        assertEquals(listOf(READ_EXTERNAL_STORAGE), MediaAccessResolver.permissionsToRequest(31))
    }

    @Test
    fun `api 33 needs both images and video for full access`() {
        assertEquals(MediaAccess.FULL, resolve(33, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
        assertEquals(MediaAccess.PARTIAL, resolve(33, READ_MEDIA_IMAGES))
        assertEquals(MediaAccess.NONE, resolve(33, READ_EXTERNAL_STORAGE))
    }

    @Test
    fun `api 34 user selected access is partial`() {
        assertEquals(MediaAccess.PARTIAL, resolve(34, READ_MEDIA_VISUAL_USER_SELECTED))
        assertEquals(MediaAccess.FULL, resolve(35, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED))
        assertEquals(MediaAccess.NONE, resolve(34))
        assertEquals(3, MediaAccessResolver.permissionsToRequest(34).size)
    }

    @Test
    fun `user selected on api 33 is ignored`() {
        assertEquals(MediaAccess.NONE, resolve(33, READ_MEDIA_VISUAL_USER_SELECTED))
    }
}
