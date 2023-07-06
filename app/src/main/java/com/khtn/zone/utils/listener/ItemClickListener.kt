package com.khtn.zone.utils.listener

import android.view.View

interface ItemClickListener {
    fun onItemClicked(v: View, position: Int)
    fun onItemLongClicked(v: View, position: Int)
}