package com.khtn.zone.model

import com.khtn.zone.utils.UserStatusConstants

data class UserStatus (
    val status: String = UserStatusConstants.OFFLINE,
    val last_seen: Long = 0,
    val typing_status: String = UserStatusConstants.NON_TYPING,
    val chat_user: String? = null)