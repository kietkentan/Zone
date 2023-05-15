package com.khtn.zone.utils

import android.os.Build
import java.util.*

object Device {
    fun generateDeviceIdentifier(): String {
        val uniqueDevicePseudoID =
            "35" + Build.BOARD.length % 10 + Build.BRAND.length % 10 +
                    Build.DEVICE.length % 10 + Build.DISPLAY.length % 10 +
                    Build.HOST.length % 10 + Build.ID.length % 10 +
                    Build.MANUFACTURER.length % 10 + Build.MODEL.length % 10 +
                    Build.PRODUCT.length % 10 + Build.TAGS.length % 10 +
                    Build.TYPE.length % 10 + Build.USER.length % 10
        val serial = Build.getRadioVersion()
        return UUID(uniqueDevicePseudoID.hashCode().toLong(), serial.hashCode().toLong()).toString()
    }
}