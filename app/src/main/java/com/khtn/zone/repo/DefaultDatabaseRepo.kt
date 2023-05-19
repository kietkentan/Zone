package com.khtn.zone.repo

import androidx.lifecycle.LiveData
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.database.data.ChatUserWithMessages
import kotlinx.coroutines.flow.Flow

interface DefaultDatabaseRepo {
    fun insertUser(user: ChatUser)
    fun insertMultipleUser(users: List<ChatUser>)
    fun getAllChatUser(): LiveData<List<ChatUser>>
    fun getChatUserList(): List<ChatUser>
    fun getChatUserById(id: String): ChatUser?
    fun deleteUserById(userId: String)
    fun getChatUserWithMessages(): Flow<List<ChatUserWithMessages>>
    fun getChatUserWithMessagesList(): List<ChatUserWithMessages>
    fun nukeTable()
}