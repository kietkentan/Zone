package com.khtn.zone.custom.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.khtn.zone.R
import com.khtn.zone.databinding.PausableProgressBinding
import com.khtn.zone.utils.printMeD

class PausableProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    @ColorInt
    var backgroundProgress = 0

    @ColorInt
    var backgroundInProgress = 0

    @ColorInt
    var backgroundMaxProgress = 0

    private val binding = PausableProgressBinding.inflate(LayoutInflater.from(context), this, true)
    private var animation: PausableScaleAnimation? = null
    private var duration = DEFAULT_PROGRESS_DURATION
    private var callback: Callback? = null
    private var isStarted = false

    fun setBackGround() {
        if (backgroundProgress != 0)
            binding.backProgress.setBackgroundColor(backgroundProgress)
        if (backgroundInProgress != 0)
            binding.frontProgress.setBackgroundColor(backgroundInProgress)
        if (backgroundMaxProgress != 0)
            binding.maxProgress.setBackgroundColor(backgroundMaxProgress)
    }

    fun setDuration(duration: Long) {
        this.duration = duration
        if (animation != null) {
            animation = null
            startProgress()
        }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setMax() {
        finishProgress(true)
    }

    fun setMin() {
        finishProgress(false)
    }

    fun setMinWithoutCallback() {
        binding.maxProgress.setBackgroundColor(backgroundMaxProgress)
        binding.maxProgress.visibility = View.VISIBLE
        if (animation != null) {
            animation!!.setAnimationListener(null)
            animation!!.cancel()
        }
    }

    fun setMaxWithoutCallback() {
        binding.maxProgress.setBackgroundColor(backgroundMaxProgress)
        binding.maxProgress.visibility = View.VISIBLE
        if (animation != null) {
            animation!!.setAnimationListener(null)
            animation!!.cancel()
        }
    }

    private fun finishProgress(isMax: Boolean) {
        if (isMax) binding.maxProgress.setBackgroundColor(backgroundMaxProgress)
        binding.maxProgress.visibility = if (isMax) View.VISIBLE else View.GONE
        if (animation != null) {
            animation!!.setAnimationListener(null)
            animation!!.cancel()
            if (callback != null) {
                callback!!.onFinishProgress()
            }
        }
    }

    fun startProgress() {
        binding.maxProgress.visibility = View.GONE
        if (duration <= 0) duration = DEFAULT_PROGRESS_DURATION
        animation =
            PausableScaleAnimation(
                0f,
                1f,
                1f,
                1f,
                Animation.ABSOLUTE,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f
            )
        animation!!.duration = duration
        animation!!.interpolator = LinearInterpolator()
        animation!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                if (isStarted) {
                    return
                }
                isStarted = true
                binding.frontProgress.visibility = View.VISIBLE
                if (callback != null) callback!!.onStartProgress()
            }

            override fun onAnimationEnd(animation: Animation) {
                isStarted = false
                if (callback != null) callback!!.onFinishProgress()
            }

            override fun onAnimationRepeat(animation: Animation) {
                //NO-OP
            }
        })
        animation!!.fillAfter = true
        binding.frontProgress.startAnimation(animation)
    }

    fun pauseProgress() {
        if (animation != null) {
            animation!!.pause()
        }
    }

    fun resumeProgress() {
        if (animation != null) {
            animation!!.resume()
        }
    }

    fun clear() {
        if (animation != null) {
            animation!!.setAnimationListener(null)
            animation!!.cancel()
            animation = null
        }
    }

    interface Callback {
        fun onStartProgress()
        fun onFinishProgress()
    }

    companion object {
        private const val DEFAULT_PROGRESS_DURATION = 4000L
    }
}