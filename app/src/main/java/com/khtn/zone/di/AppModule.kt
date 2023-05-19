package com.khtn.zone.di

import com.khtn.zone.activity.MainActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MessageCollection

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GroupCollection

@InstallIn(SingletonComponent::class)
@Module
object AppModule {
    @Provides
    fun provideMainActivity(): MainActivity {
        return MainActivity()
    }
}