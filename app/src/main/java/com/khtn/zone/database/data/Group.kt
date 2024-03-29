package com.khtn.zone.database.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.khtn.zone.model.UserProfile
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Parcelize
@Entity
data class Group(
    @PrimaryKey
    var id: String = "",
    var createdBy: String = "",
    var createdAt: Long = 0,
    var about: String = "",
    var image: String = "",
    @set:Exclude @get:Exclude var members: ArrayList<ChatUser>? = null, // only for storing in localdb
    var profiles: ArrayList<UserProfile>? = null,
    @set:Exclude @get:Exclude var unRead: Int = 0
): Parcelable