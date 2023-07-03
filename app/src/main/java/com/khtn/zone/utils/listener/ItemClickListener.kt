package com.khtn.zone.utils.listener

import android.view.View
import com.khtn.zone.model.Sticker

interface ItemClickListener {
    fun onItemClicked(v: View, position: Int)
    fun onItemLongClicked(v: View, position: Int)
    fun onStickerClicked(sticker: Sticker)
}