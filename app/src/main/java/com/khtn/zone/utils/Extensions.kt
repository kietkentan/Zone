package com.khtn.zone.utils

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

fun View.hideView() {
    visibility = View.GONE
}

fun View.hideAnimation(height: Float) {
    val mAnimator = slideAnimator(height, 0f, this)

    mAnimator?.repeatCount = 0
    mAnimator!!.addListener(object : Animator.AnimatorListener {
        override fun onAnimationEnd(animator: Animator) {
            // Height = 0, but it set visibility to GONE
            this@hideAnimation.hideView()
        }

        override fun onAnimationStart(animator: Animator) {}
        override fun onAnimationCancel(animator: Animator) {}
        override fun onAnimationRepeat(animator: Animator) {}
    })
    mAnimator.start()
}

fun View.showView() {
    visibility = View.VISIBLE
}

fun View.showAnimation(height: Float) {
    this.showView()
    val mAnimator = slideAnimator(0f, height, this)
    mAnimator!!.start()
}

fun View.disable() {
    isEnabled = false
}

fun View.enabled() {
    isEnabled = true
}

fun View.forEachChildView(closure: (View) -> Unit) {
    closure(this)
    val groupView = this as? ViewGroup ?: return
    val size = groupView.childCount - 1
    for (i in 0..size) {
        groupView.getChildAt(i).forEachChildView(closure)
    }
}

fun View.setMargin(
    left: Int? = null,
    top: Int? = null,
    right: Int? = null,
    bottom: Int? = null
) {
    val params = this.layoutParams as ViewGroup.MarginLayoutParams
    left?.let { params.leftMargin = it }
    top?.let { params.topMargin = it }
    right?.let { params.rightMargin = it }
    bottom?.let { params.bottomMargin = it }
    this.layoutParams = params
}

val EditText.value: String
    get() = this.text!!.trim().toString()

fun Fragment.toast(msg: String?) {
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

fun Activity.closeKeyBoard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    currentFocus?.let {
        imm.hideSoftInputFromWindow(it.windowToken, 0)
        it.clearFocus()
    }
}

fun Activity.showSoftKeyboard(view: View) {
    if (view.requestFocus()) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun getActivity(view: View): Activity? {
    var context = view.context
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
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

fun BottomSheetDialogFragment.setTransparentBackground() {
    dialog?.apply {
        setOnShowListener {
            val bottomSheet = findViewById<View?>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
    }
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

private fun slideAnimator(start: Float, end: Float, view: View): ValueAnimator? {
    val animator = ValueAnimator.ofFloat(start, end)
    animator.addUpdateListener { valueAnimator -> // Update Height
        val value = valueAnimator.animatedValue as Float
        val layoutParams: ViewGroup.LayoutParams = view.layoutParams
        layoutParams.height = value.toInt()
        view.layoutParams = layoutParams
    }
    return animator
}