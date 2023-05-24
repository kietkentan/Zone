package com.khtn.zone.custom.view

import android.content.Context
import android.content.res.TypedArray
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.khtn.zone.R
import com.khtn.zone.databinding.CustomInputOtpBinding
import com.khtn.zone.utils.*

interface OTPInputListener {
    fun onSuccess()
}

class OTPInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: CustomInputOtpBinding
    val listEdtOtp: MutableList<EditText>
    private var otpInputListener: OTPInputListener? = null

    var error: String?
        get() = binding.tvErrorOtpInput.text.toString()
        set(value) { if (!value.isNullOrEmpty()) binding.tvErrorOtpInput.text = value}

    var otpOne: String?
        get() = binding.edtOTP1.text.toString()
        set(value) { binding.edtOTP1.setText(value) }

    var otpTwo: String?
        get() = binding.edtOTP2.text.toString()
        set(value) { binding.edtOTP2.setText(value) }

    var otpThree: String?
        get() = binding.edtOTP3.text.toString()
        set(value) { binding.edtOTP3.setText(value) }

    var otpFour: String?
        get() = binding.edtOTP4.text.toString()
        set(value) { binding.edtOTP4.setText(value) }

    var otpFive: String?
        get() = binding.edtOTP5.text.toString()
        set(value) { binding.edtOTP5.setText(value) }

    var otpSix: String?
        get() = binding.edtOTP6.text.toString()
        set(value) { binding.edtOTP6.setText(value) }

    var currentFocus: Int = 0

    init {
        binding = CustomInputOtpBinding.inflate(LayoutInflater.from(context), this, true)
        listEdtOtp = arrayListOf(
            binding.edtOTP1,
            binding.edtOTP2,
            binding.edtOTP3,
            binding.edtOTP4,
            binding.edtOTP5,
            binding.edtOTP6
        )
        val attributes =
            context.theme.obtainStyledAttributes(attrs, R.styleable.OTPInput, defStyleAttr, 0)
        initByAttributes(attributes)
        attributes.recycle()
        initListener()
    }

    fun hideError() {
        binding.tvErrorOtpInput.hide()
    }

    fun showError() {
        binding.tvErrorOtpInput.show()
    }

    fun addListener(listener: OTPInputListener) {
        otpInputListener = listener
    }

    private fun setFocus() {
        hideError()
        val otpCode = getOTPCode()

        if (otpCode.isEmpty()) {
            currentFocus = 0
            listEdtOtp[currentFocus].requestFocus()
            getActivity(this@OTPInput)?.showSoftKeyboard(listEdtOtp[0])
        }
        else if (otpCode.length >= listEdtOtp.size && currentFocus == listEdtOtp.size - 1) {
            getActivity(this@OTPInput)?.closeKeyBoard()
            clearFocus()
            listEdtOtp[currentFocus].isCursorVisible = false
            currentFocus = -1
            otpInputListener?.onSuccess()
            return
        }
        else {
            currentFocus = otpCode.length
            if (currentFocus >= listEdtOtp.size)
                currentFocus = listEdtOtp.size - 1
            listEdtOtp[currentFocus].requestFocus()
            getActivity(this@OTPInput)?.showSoftKeyboard(listEdtOtp[currentFocus])
        }
        listEdtOtp[currentFocus].isCursorVisible = true
    }

    private fun initListener() {
        listEdtOtp[0].isCursorVisible = true
        addTextChangedListener()
        onKeyListener()
        onFocusChangeListener()
    }

    private fun initByAttributes(attributes: TypedArray) {
        error = attributes.getString(R.styleable.OTPInput_otp_error)
        otpOne = attributes.getString(R.styleable.OTPInput_otp_one)
        otpTwo = attributes.getString(R.styleable.OTPInput_otp_two)
        otpThree = attributes.getString(R.styleable.OTPInput_otp_three)
        otpFour = attributes.getString(R.styleable.OTPInput_otp_four)
        otpFive = attributes.getString(R.styleable.OTPInput_otp_five)
        otpSix = attributes.getString(R.styleable.OTPInput_otp_six)
    }

    fun setOTPCode(code: String) {
        if (code.length == 6) {
            try {
                otpOne = code[0].toString()
                otpTwo = code[1].toString()
                otpThree = code[2].toString()
                otpFour = code[3].toString()
                otpFive = code[4].toString()
                otpSix = code[5].toString()
                hideError()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addTextChangedListener() {
        for (i in 0 until listEdtOtp.size) {
            listEdtOtp[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    if (i < listEdtOtp.size - 1) listEdtOtp[i].isCursorVisible = false
                    setFocus()
                }
            })
        }
    }

    private fun onKeyListener() {
        for (i in 0 until listEdtOtp.size) {
            listEdtOtp[i].setOnKeyListener(object : OnKeyListener {
                override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                    if ((event?.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_DEL)) {
                        val code = getOTPCode()
                        if (code.isNotEmpty()) {
                            listEdtOtp[code.length - 1].setText("")
                            setFocus()
                        }
                        return true
                    } else if ((event?.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        getActivity(this@OTPInput)?.closeKeyBoard()
                        (v as EditText).clearFocus()
                        otpInputListener?.onSuccess()
                    }
                    return false
                }
            })
        }
    }

    private fun onFocusChangeListener() {
        for (i in 0 until listEdtOtp.size) {
            listEdtOtp[i].onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                if (hasFocus && i == listEdtOtp.size - 1 && !otpSix.isNullOrEmpty()) {
                    listEdtOtp[i].requestFocus()
                    listEdtOtp[i].isCursorVisible = true
                } else if (hasFocus && currentFocus != i) {
                    currentFocus = i
                    setFocus()
                }
            }
        }
    }

    fun getOTPCode(): String {
        return otpOne + otpTwo + otpThree + otpFour + otpFive + otpSix
    }
}