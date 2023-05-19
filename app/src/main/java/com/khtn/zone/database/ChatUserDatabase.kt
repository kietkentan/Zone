package com.khtn.zone.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.khtn.zone.database.dao.ChatUserDao
import com.khtn.zone.database.dao.GroupDao
import com.khtn.zone.database.dao.GroupMessageDao
import com.khtn.zone.database.dao.MessageDao
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.database.data.Group
import com.khtn.zone.database.data.GroupMessage
import com.khtn.zone.database.data.Message

@Database(
    entities = [ChatUser::class, Message::class, Group::class, GroupMessage::class],
    version = 1, exportSchema = false
)
@TypeConverters(TypeConverter::class)
abstract class ChatUserDatabase : RoomDatabase() {
    abstract fun getChatUserDao(): ChatUserDao
    abstract fun getMessageDao(): MessageDao
    abstract fun getGroupDao(): GroupDao
    abstract fun getGroupMessageDao(): GroupMessageDao
}