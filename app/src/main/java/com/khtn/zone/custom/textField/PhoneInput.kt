package com.khtn.zone.custom.textField

import android.content.Context
import android.content.res.TypedArray
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import com.khtn.zone.R
import com.khtn.zone.databinding.CustomPhoneInputBinding

class PhoneInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {
    private val binding: CustomPhoneInputBinding

    val editText: EditText
        get() = binding.edtMobile

    var text: String?
        get() = binding.edtMobile.text.toString()
        set(value) { if (!value.isNullOrEmpty()) binding.edtMobile.setText(value) }

    var textHint: String? = ""
        set(value) { if (!value.isNullOrEmpty()) binding.edtMobile.hint = value }

    var noCode: String?
        get() = binding.txtCountryCode.text.toString()
        set(value) { if (!value.isNullOrEmpty()) binding.txtCountryCode.text = value }

    var title: String?
        get() = binding.tvPhoneTitle.text.toString()
        set(value) { if (!value.isNullOrEmpty()) binding.tvPhoneTitle.text = value }

    init {
        binding = CustomPhoneInputBinding.inflate(LayoutInflater.from(context), this, true)
        val attributes =
            context.theme.obtainStyledAttributes(attrs, R.styleable.PhoneInput, defStyleAttr, 0)
        initByAttributes(attributes)
        attributes.recycle()
    }

    private fun initByAttributes(attributes: TypedArray) {
        text = attributes.getString(R.styleable.PhoneInput_text)
        textHint = attributes.getString(R.styleable.PhoneInput_text_hint)
        title = attributes.getString(R.styleable.PhoneInput_text_title)
        noCode = attributes.getString(R.styleable.PhoneInput_text_nocode)
    }

    fun onLeftClickListener(listener: OnClickListener) {
        binding.txtCountryCode.setOnClickListener(listener)
    }

    fun onKeyListener(listener: OnKeyListener) {
        binding.edtMobile.setOnKeyListener(listener)
    }

    fun addTextChangedListener(listener: TextWatcher) =
        binding.edtMobile.addTextChangedListener(listener)
}