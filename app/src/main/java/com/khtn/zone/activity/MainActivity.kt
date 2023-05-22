package com.khtn.zone.activity

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.khtn.zone.R
import com.khtn.zone.base.BaseActivity
import com.khtn.zone.databinding.ActivityMainBinding
import com.khtn.zone.utils.transparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity: BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private var backPress: Boolean = false

    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.transparentStatusBar(isLightBackground = true)

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPress) {
                    finishAffinity()
                }
                backPress = true
                Toast.makeText(this@MainActivity, R.string.press_back_exit, Toast.LENGTH_SHORT)
                    .show()
                startTimer()
            }
        })
    }

    private fun startTimer() {
        try {
            timer = object : CountDownTimer(2000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    backPress = false
                }
            }
            timer.start()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }
}