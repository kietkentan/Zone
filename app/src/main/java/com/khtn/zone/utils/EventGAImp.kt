package com.khtn.zone.utils

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.installations.FirebaseInstallations
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventGAImp @Inject constructor(
    private val mAnalytics: FirebaseAnalytics,
    mAuth: FirebaseAuth,
    private val mInstallations: FirebaseInstallations
) {
    private var id: String?

    init {
        id = mAuth.currentUser?.uid
        id?.run {
            mAnalytics.setUserId("user_$id")
        }.let {
            mInstallations.id.addOnCompleteListener {
                if (it.isSuccessful) { mAnalytics.setUserId("guest_${it.result}") }
            }.addOnFailureListener {
                mAnalytics.setUserId("guest_${Device.generateDeviceIdentifier()}")
                it.printStackTrace()
            }
        }
    }

    private fun getBaseBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.METHOD, if (id == null) "guest" else "mobile")

        return bundle
    }

    fun eventAuth(selectContent: String, type: String) {
        val bundle = getBaseBundle()
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type)
        mAnalytics.logEvent(selectContent, bundle)
    }
}