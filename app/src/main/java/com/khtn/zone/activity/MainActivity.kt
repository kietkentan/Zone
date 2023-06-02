package com.khtn.zone.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.khtn.zone.R
import com.khtn.zone.base.BaseActivity
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.database.data.Group
import com.khtn.zone.databinding.ActivityMainBinding
import com.khtn.zone.fragment.SingleChatHomeFragmentDirections
import com.khtn.zone.utils.ActionConstants
import com.khtn.zone.utils.DataConstants
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.hide
import com.khtn.zone.utils.isValidDestination
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.show
import com.khtn.zone.utils.transparentStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch


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
        observer()
    }

    @Suppress("DEPRECATION")
    private fun initView() {
        val decorView = this.window.decorView as ViewGroup
        binding.bottomNav.hide()

        try {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment?

            if (navHostFragment != null) {
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    onDestinationChanged(destination.id)
                }
                binding.bottomNav.setOnNavigationItemSelectedListener(onBottomNavigationListener)

                val isNewMessage = intent.action == ActionConstants.ACTION_NEW_MESSAGE
                val isNewGroupMessage = intent.action == ActionConstants.ACTION_GROUP_NEW_MESSAGE
                val userData = intent.getParcelableExtra<ChatUser>(DataConstants.CHAT_USER_DATA)
                val groupData = intent.getParcelableExtra<Group>(DataConstants.GROUP_DATA)

                navController = navHostFragment.navController
                if (preference.isLoggedIn() && navController.isValidDestination(R.id.loginFragment)) {
                    if (preference.getUserProfile() == null) {
                        navController.navigate(R.id.action_loginFragment_to_setupProfileFragment)
                    }
                    else
                        "navController.navigate(R.id.action_FLogIn_to_FSingleChatHome)".printMeD()
                }

                //single chat message notification clicked
                if (isNewMessage && navController.isValidDestination(R.id.singleChatHomeFragment)) {
                    preference.setCurrentUser(userData!!.id)
                    val action = SingleChatHomeFragmentDirections.actionSingleChatHomeFragmentToSingleChatFragment(userData)
                    navController.navigate(action)
                } else if (isNewGroupMessage && navController.isValidDestination(R.id.singleChatHomeFragment)) {
                    /*preference.setCurrentGroup(groupData!!.id)
                    val action = FSingleChatHomeDirections.actionFSingleChatHomeToFGroupChat(groupData)
                    navController.navigate(action)*/
                }

                if (!preference.isSameDevice())
                    Utils.showLoggedInAlert(this, preference, db)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observer() {
        val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_chats)
        badge.isVisible = false
        val groupChatBadge = binding.bottomNav.getOrCreateBadge(R.id.nav_groups)
        groupChatBadge.isVisible = false

        lifecycleScope.launch {
            groupDao.getGroupWithMessages().conflate().collect { list ->
                val count = list.filter { it.group.unRead != 0 }
                groupChatBadge.isVisible = count.isNotEmpty() // hide if 0
                groupChatBadge.number = count.size
            }
        }

        lifecycleScope.launch {
            chatUserDao.getChatUserWithMessages().conflate().collect { list ->
                val count = list.filter { it.user.unRead != 0 && it.messages.isNotEmpty() }
                badge.isVisible = count.isNotEmpty() // hide if 0
                badge.number = count.size
            }
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

    private fun isNotSameDestination(destination: Int): Boolean {
        return destination != navController.currentDestination!!.id
    }

    private fun showView() {
        binding.bottomNav.show()
    }

    private fun onDestinationChanged(currentDestination: Int) {
        try {
            when(currentDestination) {
                R.id.singleChatHomeFragment -> {
                    binding.bottomNav.selectedItemId = R.id.nav_chats
                    showView()
                }
                /*R.id.FGroupChatHome -> {
                    binding.bottomNav.selectedItemId = R.id.nav_groups
                    showView()
                }
                R.id.FSearch -> {
                    binding.bottomNav.selectedItemId = R.id.nav_contacts
                    showView()
                    binding.fab.hide()
                }
                R.id.FMyProfile -> {
                    binding.bottomNav.selectedItemId = R.id.nav_profile
                    showView()
                    binding.fab.hide()
                }*/
                else -> {
                    binding.bottomNav.hide()
                }
            }
            /*Handler(Looper.getMainLooper()).postDelayed({ //delay time for searchview
                if (this::searchItem.isInitialized) {
                    if (currentDestination == R.id.FMyProfile) {
                        searchItem.collapseActionView()
                        searchItem.isVisible = false
                    }else
                        searchItem.isVisible = true
                }
            }, 500)*/
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @Suppress("DEPRECATION")
    private val onBottomNavigationListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> {
                    val navOptions =
                        NavOptions.Builder().setPopUpTo(R.id.nav_host, true).build()
                    if (isNotSameDestination(R.id.singleChatHomeFragment)) {
                        // searchItem.collapseActionView()
                        navController.navigate(R.id.singleChatHomeFragment, null, navOptions)
                    }
                    true
                }
                /*R.id.nav_group -> {
                    if (isNotSameDestination(R.id.FGroupChatHome)) {
                        searchItem.collapseActionView()
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.FGroupChatHome)
                    }
                    true
                }
                R.id.nav_search -> {
                    if (isNotSameDestination(R.id.FSearch)) {
                        searchItem.collapseActionView()
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.FSearch)
                    }
                    true
                }
                else -> {
                    if (isNotSameDestination(R.id.FMyProfile)) {
                        searchItem.collapseActionView()
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.FMyProfile)
                    }
                    true
                }*/
                else -> { true }
            }

        }
}