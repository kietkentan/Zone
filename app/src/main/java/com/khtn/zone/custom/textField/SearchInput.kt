package com.khtn.zone.custom.textField

import android.content.Context
import android.content.res.TypedArray
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.khtn.zone.R
import com.khtn.zone.databinding.CustomSearchBarBinding

class SearchInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {
    private val binding: CustomSearchBarBinding
    private var text: String? = ""
    private var textHint: String? = ""

    init {
        binding = CustomSearchBarBinding.inflate(LayoutInflater.from(context), this, true)
        val attributes =
            context.theme.obtainStyledAttributes(attrs, R.styleable.PhoneInput, defStyleAttr, 0)
        initByAttributes(attributes)
        attributes.recycle()
        initView()
    }

    fun onTextChangeListener(watcher: TextWatcher) {
        binding.edtTextInput.addTextChangedListener(watcher)
    }

    fun onLeftClickListener(listener: OnClickListener) {
        binding.btnRight.setOnClickListener(listener)
    }

    fun getText(): String {
        return binding.edtTextInput.text.toString()
    }

    fun setText(str: String) {
        binding.edtTextInput.setText(str)
    }

    fun setTextHint(str: String) {
        binding.edtTextInput.hint = str
    }

    private fun initView() {
        if (!textHint.isNullOrEmpty()) binding.edtTextInput.hint = textHint
        if (!text.isNullOrEmpty()) binding.edtTextInput.setText(text)
    }

    private fun initByAttributes(attributes: TypedArray) {
        text = attributes.getString(R.styleable.PhoneInput_text)
        textHint = attributes.getString(R.styleable.PhoneInput_text_hint)
    }
}