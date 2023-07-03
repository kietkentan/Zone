package com.khtn.zone.utils

import android.util.Log
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.khtn.zone.model.ModelMobile


object Validator {
    fun isValidNo(
        code: String,
        mobileNo: String
    ): Boolean {
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val phNumberProto = phoneUtil.parse(mobileNo, code)
            return phoneUtil.isValidNumber(phNumberProto)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun isMobileNumberEmpty(mobileNo: String?): Boolean{
        return mobileNo.isNullOrEmpty()
    }
}