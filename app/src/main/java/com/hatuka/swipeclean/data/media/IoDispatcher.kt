package com.hatuka.swipeclean.data.media

import javax.inject.Qualifier

/** The dispatcher for blocking MediaStore/database work; swapped for a test dispatcher in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
