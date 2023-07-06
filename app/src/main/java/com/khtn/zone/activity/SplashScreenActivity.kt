@file:Suppress("DEPRECATION")

package com.khtn.zone.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.CollectionReference
import com.khtn.zone.R
import com.khtn.zone.di.UserCollection
import com.khtn.zone.utils.LocaleHelper
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.SupportLanguage
import com.khtn.zone.utils.UserUtils
import com.khtn.zone.utils.checkAppStart
import com.khtn.zone.utils.disable
import com.khtn.zone.utils.enabled
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.transparentStatusBar
import com.khtn.zone.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashScreenActivity : AppCompatActivity() {
    @Inject
    lateinit var preference: SharedPreferencesManager

    @UserCollection
    @Inject
    lateinit var userCollection: CollectionReference

    private lateinit var sharedViewModel: SharedViewModel
    private var state: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        this.transparentStatusBar(isLightBackground = true)
        //setUpFirebaseMessage();
        setPortraitScreen()
        loadLanguageConfig()

        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        UserUtils.updatePushToken(this, userCollection,false)
        sharedViewModel.onFromSplash()

        state = checkApp()
        observer()
    }

    private fun observer() {
        sharedViewModel.openMainActivity.observe(this) {
            val isLogin = preference.isLoggedIn()
            val intent = Intent(this, if (isLogin) MainActivity::class.java else OnBoardingActivity::class.java)
            if (isLogin.not()) intent.putExtra("state_app", state)
            startActivity(intent)
            finish()
        }
    }

    private fun loadLanguageConfig() {
        LocaleHelper.loadLanguageConfig(this)
    }

    private fun checkApp(): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@SplashScreenActivity)
        val str = checkAppStart(this@SplashScreenActivity, sharedPreferences)

        return str.toString()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setPortraitScreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}