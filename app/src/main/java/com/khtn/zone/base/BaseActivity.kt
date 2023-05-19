package com.khtn.zone.base

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.khtn.zone.MyApplication
import com.khtn.zone.database.ChatUserDatabase
import com.khtn.zone.database.dao.ChatUserDao
import com.khtn.zone.database.dao.GroupDao
import com.khtn.zone.database.dao.GroupMessageDao
import com.khtn.zone.database.dao.MessageDao
import com.khtn.zone.model.UserStatus
import com.khtn.zone.utils.*
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
open class BaseActivity: AppCompatActivity() {
    private var connectedRef: DatabaseReference? = null

    @Inject
    lateinit var database: FirebaseDatabase

    @Inject
    lateinit var preference: SharedPreferencesManager

    @Inject
    lateinit var chatUserDao: ChatUserDao

    @Inject
    lateinit var msgDao: MessageDao

    @Inject
    lateinit var groupDao: GroupDao

    @Inject
    lateinit var messageDao: GroupMessageDao

    @Inject
    lateinit var db: ChatUserDatabase

    private val newLogInReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ActionConstants.ACTION_LOGGED_IN_ANOTHER_DEVICE == intent.action)
                Utils.showLoggedInAlert(this@BaseActivity, preference, db)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        MyApplication.isAppRunning = true
        registerReceiver(
            newLogInReceiver,
            IntentFilter(ActionConstants.ACTION_LOGGED_IN_ANOTHER_DEVICE)
        )
        if (!preference.getUid().isNullOrEmpty())
            updateStatus()
    }

    override fun onResume() {
        MyApplication.isAppRunning = true
        super.onResume()
    }

    private fun updateStatus() {
        try {
            val uid = preference.getUid()
            val lastOnlineRef = database.getReference("/${FireStoreCollection.USER}/$uid/${FireStoreCollection.LAST_SEEN}")
            val status = database.getReference("/${FireStoreCollection.USER}/$uid/${FireStoreCollection.STATUS}")
            connectedRef = database.getReference(".info/connected")
            connectedRef?.addValueEventListener(object : ValueEventListener {
                @SuppressLint("LogNotTimber")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected: Boolean = (snapshot.value ?: false) as Boolean
                    if (connected) {
                        //Log.w(TAG.INFO, "Online status updated")
                        status.setValue(UserStatusConstants.ONLINE)
                        lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
                        status.onDisconnect().setValue(UserStatusConstants.OFFLINE)
                    }
                }

                @SuppressLint("LogNotTimber")
                override fun onCancelled(error: DatabaseError) {
                    //Log.e(TAG.ERROR, "Listener was cancelled at .info/connected ${error.message}")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProfileUpdated(event: UserStatus) {  //will be triggered only when initial profile update completed
        if (event.status == UserStatusConstants.ONLINE)
            updateStatus()
        else {
            val uid = preference.getUid()
            val status = database.getReference("/${FireStoreCollection.USER}/$uid/${FireStoreCollection.STATUS}")
            status.setValue(UserStatusConstants.OFFLINE)
        }
    }

    override fun onDestroy() {
        MyApplication.isAppRunning = false
        EventBus.getDefault().unregister(this)
        Timber.v("onDestroy")
        unregisterReceiver(newLogInReceiver)
        super.onDestroy()
    }
}