package com.khtn.zone.custom

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.khtn.zone.custom.imageView.ImageProfile
import com.khtn.zone.custom.textField.PhoneInput
import com.khtn.zone.custom.textField.SearchInput
import com.khtn.zone.custom.view.OTPInput
import com.khtn.zone.utils.ImageUtils
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.showView

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
    if (textPhoneError.isNullOrEmpty()) phoneInput.binding.tvErrorPhoneInput.hideView()
    else {
        phoneInput.binding.tvErrorPhoneInput.text = textPhoneError
        phoneInput.binding.tvErrorPhoneInput.showView()
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
    if (otpError.isNullOrEmpty()) otpInput.binding.tvErrorOtpInput.hideView()
    else {
        otpInput.binding.tvErrorOtpInput.text = otpError
        otpInput.binding.tvErrorOtpInput.showView()
    }
}

@BindingAdapter(
    value = ["profileUrl", "profileUri", "profileProgress"],
    requireAll = false
)
fun setValue(
    imageProfile: ImageProfile,
    profileUri: String?,
    profileUrl: String?,
    profileProgress: Boolean?
) {
    profileUri?.let {
        imageProfile.binding.igvProfile.scaleType = ImageView.ScaleType.CENTER_CROP
        ImageUtils.loadUserImage(imageProfile.binding.igvProfile, it)

    }
    profileUrl?.let {
        imageProfile.binding.igvProfile.scaleType = ImageView.ScaleType.CENTER_CROP
        ImageUtils.loadUserImage(imageProfile.binding.igvProfile, it)
    }
    if (profileProgress == null || !profileProgress) imageProfile.binding.progressProfile.hideView()
    else imageProfile.binding.progressProfile.showView()
}

@BindingAdapter(
    value = ["errorString"],
    requireAll = false
)
fun setValue(
    textView: TextView,
    errorString: String?
) {
    if (errorString.isNullOrEmpty()) textView.hideView()
    else {
        textView.text = errorString
        textView.showView()
    }
}

/*
@BindingAdapter(
    value = ["headerTitle", "headerRightIcon"],
    requireAll = false
)
fun setValue(
    header: Header,
    headerTitle: String?,
    headerRightImage: String?,
    headerRightIcon:  Drawable?
) {
    headerTitle?.let { header.binding.tvHeaderTitle.text = it }
    headerRightImage?.let { ImageUtils.loadUserImage(header.binding.ivHeaderRightIcon, it) }
    headerRightIcon?.let { header.binding.ivHeaderRightIcon.setImageDrawable(it) }
}*/
