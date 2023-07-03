package com.khtn.zone.database.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.khtn.zone.model.Sticker
import com.khtn.zone.utils.ImageTypeConstants
import com.khtn.zone.utils.MessageStatusConstants
import com.khtn.zone.utils.MessageTypeConstants
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@kotlinx.parcelize.Parcelize
@Entity
data class Message(
    @PrimaryKey
    val createdAt: Long,
    var deliveryTime: Long = 0L,
    var seenTime: Long = 0L,
    val from: String,
    val to: String,
    val senderName: String,
    val senderImage: String,
    var type: String = MessageTypeConstants.TEXT,  // 0 = text, 1 = audio, 2 = image, 3 = video, 4 = file
    var status: Int = MessageStatusConstants.SENDING,    // 0 = sending, 1 = sent, 2 = delivered, 3 = seen, 4 = failed
    var textMessage: TextMessage? = null,
    var imageMessage: ImageMessage? = null,
    var audioMessage: AudioMessage? = null,
    var videoMessage: VideoMessage? = null,
    var fileMessage: FileMessage? = null,
    var chatUsers: ArrayList<String>? = null,
    @set:Exclude @get:Exclude
    var chatUserId: String? = null
): Parcelable

@Serializable
@kotlinx.parcelize.Parcelize
data class TextMessage(
    val text: String? = null
): Parcelable

@Serializable
@kotlinx.parcelize.Parcelize
data class AudioMessage(
    var uri: String? = null,
    val duration: Int = 0
): Parcelable

@Serializable
@kotlinx.parcelize.Parcelize
data class ImageMessage(
    var uri: String? = null,
    var imageType: String = ImageTypeConstants.IMAGE,
    var sticker: Sticker? = null,
    var isGiftSticker: Boolean? = false
): Parcelable

@Serializable
@kotlinx.parcelize.Parcelize
data class VideoMessage(
    val uri: String? = null,
    val duration: Int = 0
): Parcelable

@Serializable
@kotlinx.parcelize.Parcelize
data class FileMessage(
    val name: String? = null,
    val uri: String? = null,
    val duration: Int = 0
): Parcelable
