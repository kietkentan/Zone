package com.khtn.zone.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
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
import com.khtn.zone.utils.ContactUtils
import com.khtn.zone.utils.DataConstants
import com.khtn.zone.utils.LocaleHelper
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.closeKeyBoard
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.isValidDestination
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.showView
import com.khtn.zone.utils.toast
import com.khtn.zone.utils.transparentStatusBar
import com.khtn.zone.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity: BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private var navHostFragment: NavHostFragment? = null
    private lateinit var navController: NavController
    private var doubleBackPress: Boolean = false

    private val sharedViewModel: SharedViewModel by viewModels()

    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadLanguageConfig()
        this.transparentStatusBar(isLightBackground = true)

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.isValidDestination(R.id.singleChatHomeFragment) ||
                    navController.isValidDestination(R.id.setupProfileFragment)) {
                    if (doubleBackPress) finishAffinity()
                    doubleBackPress = true
                    baseContext.toast(getString(R.string.press_back_exit))
                    startTimer()
                }
                /*else if (navController.isValidDestination(R.id.FMyProfile) ||
                    navController.isValidDestination(R.id.FGroupChatHome) ||
                    navController.isValidDestination(R.id.FSearch)) {
                    val navOptions = NavOptions.Builder().setPopUpTo(R.id.nav_host, true).build()
                    navController.navigate(R.id.singleChatHomeFragment, null, navOptions)
                }*/
                else navController.popBackStack()
            }
        })

        binding.floatingButtonMain.setOnClickListener {
            /*if (searchItem.isActionViewExpanded)
                searchItem.collapseActionView()*/
            ContactUtils.askContactPermission(returnFragment()!!) {
                /*if (navController.isValidDestination(R.id.singleChatHomeFragment))
                    navController.navigate(R.id.action_FSingleChatHome_to_FContacts)
                else */if (navController.isValidDestination(R.id.groupChatHomeFragment))
                    navController.navigate(R.id.action_groupChatHomeFragment_to_createGroupChatFragment)
            }
        }

        initView()
        observer()
    }

    private fun returnFragment(): Fragment? {
        val navHostFragment: Fragment? =
            supportFragmentManager.findFragmentById(R.id.nav_host)
        return navHostFragment?.childFragmentManager?.fragments?.get(0)
    }

    private fun loadLanguageConfig() {
        LocaleHelper.loadLanguageConfig(this)
    }

    @Suppress("DEPRECATION")
    private fun initView() {
        try {
            navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment?

            if (navHostFragment != null) {
                navController = navHostFragment!!.navController
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    onDestinationChanged(destination.id)
                }
                binding.bottomNav.setOnNavigationItemSelectedListener(onBottomNavigationListener)

                val isNewMessage = intent.action == ActionConstants.ACTION_NEW_MESSAGE
                val isNewGroupMessage = intent.action == ActionConstants.ACTION_GROUP_NEW_MESSAGE
                val userData = intent.getParcelableExtra<ChatUser>(DataConstants.CHAT_USER_DATA)
                val groupData = intent.getParcelableExtra<Group>(DataConstants.GROUP_DATA)

                if (preference.isLoggedIn() && navController.isValidDestination(R.id.loginFragment)) {
                    if (preference.getUserProfile() == null) {
                        navController.navigate(R.id.action_loginFragment_to_setupProfileFragment)
                    } else
                        navController.navigate(R.id.action_loginFragment_to_singleChatHomeFragment)
                }

                // single chat message notification clicked
                if (isNewMessage && navController.isValidDestination(R.id.singleChatHomeFragment)) {
                    preference.setCurrentUser(userData!!.id)
                    val action =
                        SingleChatHomeFragmentDirections.actionSingleChatHomeFragmentToSingleChatFragment(userData)
                    navController.navigate(action)
                } else if (isNewGroupMessage && navController.isValidDestination(R.id.singleChatHomeFragment)) {
                    preference.setCurrentGroup(groupData!!.id)
                    val action =
                        SingleChatHomeFragmentDirections.actionSingleChatHomeFragmentToGroupChatFragment(groupData)
                    navController.navigate(action)
                }

                if (!preference.isSameDevice())
                    Utils.showLoggedInAlert(this, preference, db)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showLogoutAlert() {
        Utils.showLogoutAlert(this, preference, db)
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
                    doubleBackPress = false
                }
            }
            timer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.fragments.first() as? NavHostFragment
        if (navHostFragment != null) {
            val childFragments = navHostFragment!!.childFragmentManager.fragments
            childFragments.forEach { fragment ->
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (navHostFragment != null) {
            val childFragments = navHostFragment!!.childFragmentManager.fragments
            childFragments.forEach { fragment ->
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun goToSettings() {
        val myAppSettings = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT)
        // myAppSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityForResult(myAppSettings, Utils.REQUEST_APP_SETTINGS)
    }

    private fun isNotSameDestination(destination: Int): Boolean {
        return destination != navController.currentDestination!!.id
    }

    private fun showView() {
        binding.bottomNav.showView()
        binding.floatingButtonMain.showView()
    }

    private fun onDestinationChanged(currentDestination: Int) {
        try {
            when (currentDestination) {
                R.id.singleChatHomeFragment -> {
                    binding.bottomNav.selectedItemId = R.id.nav_chats
                    showView()
                }

                R.id.groupChatHomeFragment -> {
                    binding.bottomNav.selectedItemId = R.id.nav_groups
                    showView()
                }

                R.id.contactFragment -> {
                    binding.bottomNav.selectedItemId = R.id.nav_contacts
                    showView()
                    binding.floatingButtonMain.hideView()
                }

                R.id.profileFragment -> {
                    binding.bottomNav.selectedItemId = R.id.nav_profile
                    showView()
                    binding.floatingButtonMain.hideView()
                }

                else -> {
                    binding.bottomNav.hideView()
                    binding.floatingButtonMain.hideView()
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

                R.id.nav_groups -> {
                    if (isNotSameDestination(R.id.groupChatHomeFragment)) {
                        // searchItem.collapseActionView()
                        navController.navigate(R.id.groupChatHomeFragment)
                    }
                    true
                }

                R.id.nav_contacts -> {
                    if (isNotSameDestination(R.id.contactFragment)) {
                        // searchItem.collapseActionView()
                        navController.navigate(R.id.contactFragment)
                    }

                    true
                }

                R.id.nav_profile -> {
                    if (isNotSameDestination(R.id.profileFragment)) {
                        // searchItem.collapseActionView()
                        navController.navigate(R.id.profileFragment)
                    }

                    true
                }

                else -> {
                    true
                }
            }
        }
}