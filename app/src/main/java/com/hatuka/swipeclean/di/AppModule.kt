package com.hatuka.swipeclean.di

import android.content.ContentResolver
import android.content.Context
import com.hatuka.swipeclean.data.media.IoDispatcher
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.data.media.MediaStoreRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun contentResolver(@ApplicationContext context: Context): ContentResolver = context.contentResolver

    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun mediaRepository(impl: MediaStoreRepository): MediaRepository
}
