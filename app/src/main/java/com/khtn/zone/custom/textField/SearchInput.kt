package com.khtn.zone.custom.textField

import android.content.Context
import android.content.res.TypedArray
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.khtn.zone.R
import com.khtn.zone.databinding.CustomSearchBarBinding

class SearchInput: LinearLayout {
    val binding = CustomSearchBarBinding.inflate(LayoutInflater.from(context), this, true)
    var textSearch: String? = null
    var textSearchHint: String? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SearchInput)
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

    private fun initView() {
        if (!textSearchHint.isNullOrEmpty()) binding.edtTextInput.hint = textSearchHint
        if (!textSearch.isNullOrEmpty()) binding.edtTextInput.setText(textSearch)
    }

    private fun initByAttributes(attributes: TypedArray) {
        textSearch = attributes.getString(R.styleable.SearchInput_textSearch)
        textSearchHint = attributes.getString(R.styleable.SearchInput_textSearchHint)
    }
}