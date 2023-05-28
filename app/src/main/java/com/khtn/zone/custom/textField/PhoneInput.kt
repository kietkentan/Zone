package com.khtn.zone.custom.textField

import android.content.Context
import android.content.res.TypedArray
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.khtn.zone.R
import com.khtn.zone.databinding.CustomPhoneInputBinding

class PhoneInput: LinearLayout {
    val binding = CustomPhoneInputBinding.inflate(LayoutInflater.from(context), this, true)

    var textPhone: String? = null
    var textPhoneHint: String? = null
    var textNoCode: String? = null
    var textPhoneTitle: String? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.PhoneInput)
        initByAttributes(attributes)
        attributes.recycle()
        initView()
    }


    private fun initByAttributes(attributes: TypedArray) {
        textPhone = attributes.getString(R.styleable.PhoneInput_textPhone)
        textPhoneHint = attributes.getString(R.styleable.PhoneInput_textPhoneHint)
        textPhoneTitle = attributes.getString(R.styleable.PhoneInput_textPhoneTitle)
        textNoCode = attributes.getString(R.styleable.PhoneInput_textNoCode)
    }

    private fun initView() {
        textPhone?.let { binding.edtMobile.setText(it) }
        textPhoneHint?.let { binding.edtMobile.hint = it }
        textNoCode?.let { binding.tvCountryCode.text = it }
        textPhoneTitle?.let { binding.tvPhoneTitle.text = it }
    }

    fun onLeftClickListener(listener: OnClickListener) {
        binding.tvCountryCode.setOnClickListener(listener)
    }

    fun onKeyListener(listener: OnKeyListener) {
        binding.edtMobile.setOnKeyListener(listener)
    }

    fun addTextChangedListener(listener: TextWatcher) =
        binding.edtMobile.addTextChangedListener(listener)
}