package com.khtn.zone.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment

fun View.hide() {
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.disable() {
    isEnabled = false
}

fun View.enabled() {
    isEnabled = true
}

fun Fragment.toast(msg: String?) {
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

fun Activity.closeKeyBoard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    currentFocus?.let {
        imm.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

fun Activity.showSoftKeyboard(view: View) {
    if (view.requestFocus()) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

@SuppressLint("InlinedApi")
@Suppress("DEPRECATION")
fun Activity.transparentStatusBar(isLightBackground: Boolean) {
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    if (isLightBackground)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

    window.statusBarColor = Color.TRANSPARENT
}

fun Context.getAppName(): String = applicationInfo.loadLabel(packageManager).toString()

val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Float.dpToPx: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

val Float.pxToDp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)