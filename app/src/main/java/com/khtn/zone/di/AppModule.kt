package com.khtn.zone.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.installations.FirebaseInstallations
import com.khtn.zone.utils.EventGA
import com.khtn.zone.utils.EventGAImp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppModule {
    /*
    @Provides
    @Singleton
    fun provideEventGA(
        mAnalytics: FirebaseAnalytics,
        mAuth: FirebaseAuth,
        mInstallations: FirebaseInstallations
    ): EventGA {
        return EventGAImp(mAnalytics, mAuth, mInstallations)
    }
    */
}