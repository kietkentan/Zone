package com.khtn.zone.database.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.khtn.zone.model.UserProfile
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Parcelize
@Entity
data class ChatUser(
    @PrimaryKey
    var id: String,
    var localName: String,
    var user: UserProfile,
    var documentId: String? = null,
    var locallySaved: Boolean = false,
    var unRead: Int = 0,
    var isSearchedUser: Boolean = true,
    var isSelected: Boolean = false
): Parcelable