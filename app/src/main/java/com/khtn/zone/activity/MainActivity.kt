package com.khtn.zone.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.khtn.zone.R
import com.khtn.zone.base.BaseActivity
import com.khtn.zone.databinding.ActivityMainBinding
import com.khtn.zone.utils.isValidDestination
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.transparentStatusBar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity: BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
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
        initView()
    }

    private fun initView() {
        try {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment?

            if (navHostFragment != null) {
                navController = navHostFragment.navController
                if (preference.isLoggedIn() && navController.isValidDestination(R.id.loginFragment)) {
                    if (preference.getUserProfile() == null) {
                        navController.navigate(R.id.action_loginFragment_to_setupProfileFragment)
                    }
                    else
                        "navController.navigate(R.id.action_FLogIn_to_FSingleChatHome)".printMeD()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val navHostFragment = supportFragmentManager.fragments.first() as? NavHostFragment
        if (navHostFragment != null) {
            val childFragments = navHostFragment.childFragmentManager.fragments
            childFragments.forEach { fragment ->
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }
    }
}