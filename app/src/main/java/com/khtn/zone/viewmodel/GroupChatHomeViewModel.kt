package com.khtn.zone.viewmodel

import androidx.lifecycle.ViewModel
import com.khtn.zone.repo.DatabaseRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GroupChatHomeViewModel @Inject constructor(private val dbRepo: DatabaseRepo): ViewModel()  {
    fun getGroupMessages() = dbRepo.getGroupWithMessages()
    fun getGroupMessagesAsList() = dbRepo.getGroupWithMessagesList()
}