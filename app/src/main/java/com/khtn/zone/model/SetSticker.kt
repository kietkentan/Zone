package com.khtn.zone.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.khtn.zone.utils.ImageTypeConstants
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
@Parcelize
class SetSticker(
    val id: String = "",
    val name: String = "",
    val image: String = "",
    val type: String = ImageTypeConstants.IMAGE
): Parcelable