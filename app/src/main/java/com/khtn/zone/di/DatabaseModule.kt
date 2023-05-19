package com.khtn.zone.di

import android.content.Context
import androidx.room.Room
import com.khtn.zone.database.ChatUserDatabase
import com.khtn.zone.utils.DataConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideChatUserDatabase(@ApplicationContext context: Context): ChatUserDatabase {
        return Room.databaseBuilder(context, ChatUserDatabase::class.java,
            DataConstants.CHAT_USER_DB_NAME)
            .build()
    }

    @Singleton
    @Provides
    fun provideChatUserDao(db: ChatUserDatabase) = db.getChatUserDao()

    @Singleton
    @Provides
    fun provideMessageDao(db: ChatUserDatabase) = db.getMessageDao()

    @Singleton
    @Provides
    fun provideGroupDao(db: ChatUserDatabase) = db.getGroupDao()

    @Singleton
    @Provides
    fun provideGroupMessageDao(db: ChatUserDatabase) = db.getGroupMessageDao()
}