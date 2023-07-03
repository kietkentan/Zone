package com.khtn.zone.utils.listener

import com.khtn.zone.model.Sticker

interface StickerListener {
    fun onStickerMessageClicked(sticker: Sticker)
    fun onStickerSetClicked(sticker: Sticker)
}