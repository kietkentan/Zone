package com.khtn.zone.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.khtn.zone.R
import com.khtn.zone.utils.AttachmentOptions

class Attachment(
    @DrawableRes
    val image: Int,
    @StringRes
    val title: Int,
    val pos: Int
) {
    companion object {
        fun getData(): List<Attachment> {
            return listOf(
                Attachment(R.drawable.ic_add_image, R.string.attachment_add_image, AttachmentOptions.IMAGE_VIDEO),
                Attachment(R.drawable.ic_add_file, R.string.attachment_add_file, AttachmentOptions.FILE),
                Attachment(R.drawable.ic_quick_message, R.string.attachment_quick_message, AttachmentOptions.QUICK_MESSAGE)
            )
        }
    }
}