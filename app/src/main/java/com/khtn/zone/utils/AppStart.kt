@file:Suppress("DEPRECATION")

package com.khtn.zone.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.util.Log
import com.khtn.zone.BuildConfig

enum class AppStart {
    FIRST_TIME, FIRST_TIME_VERSION, NORMAL
}

/**
 * The app version code (not the version name!) that was used on the last start of the app.
 */
private const val LAST_APP_VERSION = BuildConfig.VERSION_CODE.toString()

/**
 * Caches the result of [.checkAppStart]. To allow idempotent method calls.
 */
private var appStart: AppStart? = null

/**
 * Finds out started for the first time (ever or in the current version).
 * @return the type of app start
 */
@SuppressLint("LogNotTimber")
fun checkAppStart(
    context: Context,
    sharedPreferences: SharedPreferences
): AppStart? {
    val pInfo: PackageInfo
    try {
        pInfo = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        )
        val lastVersionCode = sharedPreferences.getInt(
            LAST_APP_VERSION, -1
        )

        val currentVersionCode = pInfo.versionCode
        appStart = checkAppStart(currentVersionCode, lastVersionCode)

        // Update version in preferences
        sharedPreferences.edit()
            .putInt(LAST_APP_VERSION, currentVersionCode)
            .apply() // must use commit here or app may not update prefs in time and app will loop into walk through
    } catch (e: NameNotFoundException) {
        Log.w(
            "TAG_U",
            "Unable to determine current app version from package manager. Defensively assuming normal app start."
        )
    }
    return appStart
}

@SuppressLint("LogNotTimber")
fun checkAppStart(
    currentVersionCode: Int,
    lastVersionCode: Int
): AppStart {
    return if (lastVersionCode == -1) {
        AppStart.FIRST_TIME
    } else if (lastVersionCode < currentVersionCode) {
        AppStart.FIRST_TIME_VERSION
    } else if (lastVersionCode > currentVersionCode) {
        Log.w(
            "TAG_U",
            "Current version code ($currentVersionCode) is less then the one recognized on last startup ($lastVersionCode). Defensively assuming normal app start."
        )
        AppStart.NORMAL
    } else {
        AppStart.NORMAL
    }
}