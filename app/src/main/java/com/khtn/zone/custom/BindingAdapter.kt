package com.khtn.zone.custom

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.khtn.zone.custom.imageView.ImageProfile
import com.khtn.zone.custom.textField.PhoneInput
import com.khtn.zone.custom.textField.SearchInput
import com.khtn.zone.custom.view.OTPInput
import com.khtn.zone.utils.ImageUtils
import com.khtn.zone.utils.hide
import com.khtn.zone.utils.show

@BindingAdapter(
    value = ["textPhone", "textPhoneHint", "textNoCode", "textPhoneTitle", "textPhoneError"],
    requireAll = false
)
fun setValue(
    phoneInput: PhoneInput,
    textPhone: String?,
    textPhoneHint: String?,
    textNoCode: String?,
    textPhoneTitle: String?,
    textPhoneError: String?
) {
    textPhone?.let { phoneInput.binding.edtMobile.setText(it) }
    textPhoneHint?.let { phoneInput.binding.edtMobile.hint = it }
    textNoCode?.let { phoneInput.binding.tvCountryCode.text = it }
    textPhoneTitle?.let { phoneInput.binding.tvPhoneTitle.text = it }
    if (textPhoneError.isNullOrEmpty()) phoneInput.binding.tvErrorPhoneInput.hide()
    else {
        phoneInput.binding.tvErrorPhoneInput.text = textPhoneError
        phoneInput.binding.tvErrorPhoneInput.show()
    }
}

@BindingAdapter(
    value = ["textSearch", "textSearchHint"],
    requireAll = false
)
fun setValue(
    searchInput: SearchInput,
    textSearch: String?,
    textSearchHint: String?
) {
    textSearch?.let { searchInput.binding.edtTextInput.setText(it) }
    textSearchHint?.let { searchInput.binding.edtTextInput.hint = it }
}

@BindingAdapter(
    value = ["otpError"],
    requireAll = false
)
fun setValue(
    otpInput: OTPInput,
    otpError: String?
) {
    if (otpError.isNullOrEmpty()) otpInput.binding.tvErrorOtpInput.hide()
    else {
        otpInput.binding.tvErrorOtpInput.text = otpError
        otpInput.binding.tvErrorOtpInput.show()
    }
}

@BindingAdapter(
    value = ["profileUrl", "profileProgress"],
    requireAll = false
)
fun setValue(
    imageProfile: ImageProfile,
    profileUrl: String?,
    profileProgress: Boolean?
) {
    profileUrl?.let { ImageUtils.loadUserImage(imageProfile.binding.igvProfile, it) }
    if (profileProgress == null || !profileProgress) imageProfile.binding.progressProfile.hide()
    else imageProfile.binding.progressProfile.show()
}

@BindingAdapter(
    value = ["errorString"],
    requireAll = false
)
fun setValue(
    textView: TextView,
    errorString: String?
) {
    if (errorString.isNullOrEmpty()) textView.hide()
    else {
        textView.text = errorString
        textView.show()
    }
}