package com.khtn.zone.custom.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.khtn.zone.R
import com.khtn.zone.databinding.ViewHeaderBinding
import com.khtn.zone.utils.ImageUtils

class Header: LinearLayout {
    val binding = ViewHeaderBinding.inflate(LayoutInflater.from(context), this, true)
    var title: String? = null
    var iconAvatar: String? = null
    var iconDrawable: Drawable? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.Header)
        initByAttributes(attributes)
        attributes.recycle()
        initView()
    }

    fun setRightImageUrl(url: String) {
        iconAvatar = url
        ImageUtils.loadUserImage(binding.ivHeaderRightIcon, url)
    }

    private fun initView() {
        title?.let { binding.tvHeaderTitle.text = it }
        iconAvatar?.let { ImageUtils.loadUserImage(binding.ivHeaderRightIcon, it) }
        iconDrawable?.let { binding.ivHeaderRightIcon.setImageDrawable(it) }
    }

    private fun initByAttributes(attributes: TypedArray) {
        title = attributes.getString(R.styleable.Header_headerTitle)
        iconAvatar = attributes.getString(R.styleable.Header_headerRightIcon)
        iconDrawable = attributes.getDrawable(R.styleable.Header_headerRightIcon)
    }

    fun setRightIconClickListener(unit: Unit) {
        binding.ivHeaderRightIcon.setOnClickListener{ unit }
    }
}