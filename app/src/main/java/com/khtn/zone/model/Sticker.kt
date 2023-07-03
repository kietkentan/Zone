package com.khtn.zone.model

import android.os.Parcelable
import com.khtn.zone.utils.ImageTypeConstants
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
class Sticker(
    val id: String = "",
    val url: String = "",
    val setStickerId: String = "",
    val type: String = ImageTypeConstants.IMAGE
): Parcelable