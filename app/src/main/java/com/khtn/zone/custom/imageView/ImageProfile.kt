package com.khtn.zone.custom.imageView

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.khtn.zone.R
import com.khtn.zone.databinding.CustomImageProfileBinding
import com.khtn.zone.utils.ImageUtils
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.showView

class ImageProfile: RelativeLayout{
    val binding = CustomImageProfileBinding.inflate(LayoutInflater.from(context), this, true)

    var profileUrl: String? = null
    var profileProgress: Boolean = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ImageProfile)
        initByAttributes(attributes)
        attributes.recycle()
        onClickListener()
    }

    private fun initByAttributes(attributes: TypedArray) {
        profileUrl = attributes.getString(R.styleable.ImageProfile_profileUrl)
        profileProgress = attributes.getBoolean(R.styleable.ImageProfile_profileProgress, false)
    }
    private fun onClickListener() {
        profileUrl?.let { ImageUtils.loadUserImage(binding.igvProfile, it) }
        if (!profileProgress) binding.progressProfile.hideView()
        else binding.progressProfile.showView()
    }
}