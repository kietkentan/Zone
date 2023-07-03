package com.khtn.zone

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.lifecycle.LifecycleObserver
import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.UserUtils
import dagger.hilt.android.HiltAndroidApp
import com.khtn.zone.utils.SharedPrefConstants
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.database.dao.ChatUserDao
import com.khtn.zone.database.dao.MessageDao
import com.khtn.zone.di.UserCollection
import com.khtn.zone.utils.LocaleHelper
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MyApplication: MultiDexApplication(), LifecycleObserver, Configuration.Provider {
    @Inject
    lateinit var preference: SharedPreferencesManager

    @Inject
    lateinit var userDao: ChatUserDao

    @Inject
    lateinit var messageDao: MessageDao

    @UserCollection
    @Inject
    lateinit var userCollection: CollectionReference

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        lateinit var instance: MyApplication
            private set
        var isAppRunning = false
        lateinit var appContext: Context
        lateinit var userDaoo: ChatUserDao
        lateinit var messageDaoo: MessageDao

        private var maxWidth = 0
        private var maxHeight = 0

        fun setMaxWidth(w: Int) {
            if (w > maxWidth) {
                maxWidth = w
            }
        }

        fun setMaxHeight(h: Int) {
            if (h > maxHeight) {
                maxHeight = h
            }
        }

        fun getMaxWidth(): Int = maxWidth

        fun getMaxHeight(): Int = maxHeight
    }

    @Suppress("DEPRECATION")
    private fun setDisplaySize() {
        val windowManager =
            applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        setMaxWidth(size.x)
        setMaxHeight(size.y)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            loadLanguageConfig()
            instance = this
            appContext = this
            userDaoo = userDao
            messageDaoo = messageDao

            FirebaseApp.initializeApp(this)
            /*val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )*/
            initTimber()
            setDisplaySize()
            if (preference.retrieveBooleanByKey(SharedPrefConstants.LOGIN))
                checkLastDevice()   // looking for does user is logged in another device.if yes,need to shoe dialog for log in again
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLanguageConfig() {
        LocaleHelper.loadLanguageConfig(this)
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "LetsChat/${element.fileName}:${element.lineNumber})#${element.methodName}"
                }
            })
        }
    }

    @SuppressLint("LogNotTimber")
    private fun checkLastDevice() {
        userCollection.document(preference.retrieveStringByKey(SharedPrefConstants.UID)!!).get().addOnSuccessListener { data ->
            Timber.v("Device Checked")
            val appUser = data.toObject(UserProfile::class.java)
            checkDeviceDetails(appUser)
        }.addOnFailureListener { e ->
            e.printStackTrace()
        }
    }

    private fun checkDeviceDetails(appUser: UserProfile?) {
        val device = appUser?.deviceDetails
        val localDevice = UserUtils.getDeviceId(this)
        if (device != null) {
            val sameDevice = device.device_id.equals(localDevice)
            preference.saveBooleanByKey(SharedPrefConstants.LAST_LOGGED_DEVICE_SAME, sameDevice)
            Timber.v("Device Checked ${device.device_id.equals(localDevice)}")
            if (sameDevice)
                UserUtils.updatePushToken(this, userCollection, true)
        }
    }
}