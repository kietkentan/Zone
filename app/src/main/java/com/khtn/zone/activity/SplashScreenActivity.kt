@file:Suppress("DEPRECATION")

package com.khtn.zone.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.khtn.zone.R
import com.khtn.zone.utils.LocaleHelper
import com.khtn.zone.utils.checkAppStart
import com.khtn.zone.utils.transparentStatusBar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.concurrent.schedule

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLanguageConfig()
        setContentView(R.layout.activity_splash_screen)
        this.transparentStatusBar(isLightBackground = true)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        // setUpFirebaseMessage();
        setPortraitScreen()
        checkApp()
    }

    private fun checkApp() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@SplashScreenActivity)
        val str = checkAppStart(this@SplashScreenActivity, sharedPreferences)

        Timer().schedule(1000L){
            val intent = Intent(this@SplashScreenActivity, OnBoardingActivity::class.java)
            intent.putExtra("state_app", str.toString())
            startActivity(intent)
            finish()
        }
    }

    private fun loadLanguageConfig() {
        LocaleHelper.loadLanguageConfig(this)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setPortraitScreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}