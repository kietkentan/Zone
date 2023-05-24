package com.khtn.zone.custom.imageView

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.net.toUri
import com.khtn.zone.R
import com.khtn.zone.databinding.CustomImageProfileBinding

interface ChoseSuccess {
    fun onSuccess()
}

class ImageProfile @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): RelativeLayout(context, attrs, defStyleAttr){
    private val binding: CustomImageProfileBinding
    private var choseSuccess: ChoseSuccess? = null

    var url: String? = null
        set(value) { if (!value.isNullOrEmpty()) binding.igvProfile.setImageURI(value.toUri()) }

    var progress: Boolean
        get() = binding.progressProfile.visibility == View.VISIBLE
        set(value) { binding.progressProfile.visibility = if (value) View.VISIBLE else View.GONE }

    init {
        binding = CustomImageProfileBinding.inflate(LayoutInflater.from(context), this, true)
        val attributes =
            context.theme.obtainStyledAttributes(attrs, R.styleable.ImageProfile, defStyleAttr, 0)
        initByAttributes(attributes)
        attributes.recycle()
        onClickListener()
    }

    private fun initByAttributes(attributes: TypedArray) {
        url = attributes.getString(R.styleable.ImageProfile_url_profile)
        progress = attributes.getBoolean(R.styleable.ImageProfile_progress_profile, false)
    }

    private fun onClickListener() {

    }

    fun onChoseImageSuccess(listener: ChoseSuccess) {
        choseSuccess = listener
    }
}