package com.khtn.zone.di

import com.khtn.zone.database.dao.ChatUserDao
import com.khtn.zone.database.dao.GroupDao
import com.khtn.zone.database.dao.GroupMessageDao
import com.khtn.zone.database.dao.MessageDao
import com.khtn.zone.repo.DatabaseRepo
import com.khtn.zone.repo.DefaultDatabaseRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
object RepoModule {
    @Provides
    fun provideDefaultDatabaseRepo(
        userDao: ChatUserDao,
        groupDao: GroupDao,
        groupMsgDao: GroupMessageDao,
        messageDao: MessageDao
    ): DefaultDatabaseRepo {
        return DatabaseRepo(userDao, groupDao, groupMsgDao, messageDao)
    }
}