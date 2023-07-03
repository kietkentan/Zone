package com.khtn.zone.utils

import android.Manifest
import androidx.fragment.app.Fragment

object ContactUtils {
    const val REQUEST_CONTACT_PERMISSION = 22

    val CONTACT_PERMISSION = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    )

    fun askContactPermission(
        context: Fragment,
        actionIsPermission: () -> Unit
    ) {
        if (checkContactPermission(context))
            actionIsPermission.invoke()
    }

    private fun checkContactPermission(context: Fragment): Boolean {
        return Utils.checkPermission(
            context = context,
            permissions = CONTACT_PERMISSION,
            reqCode = REQUEST_CONTACT_PERMISSION
        )
    }
}