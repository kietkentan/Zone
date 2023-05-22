@file:Suppress("DEPRECATION")

package com.khtn.zone.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.khtn.zone.R
import com.khtn.zone.database.ChatUserDatabase
import com.khtn.zone.database.data.GroupMessage
import com.khtn.zone.model.Country
import com.khtn.zone.model.UserStatus
import java.text.SimpleDateFormat

object Utils {
    private const val PERMISSION_REQ_CODE = 114

    fun getDefaultCountry() = Country("VN", "Vietnam", "+84", "VND")

    fun clearNull(str: String?) = str?.trim() ?: ""

    @Suppress("DEPRECATION")
    fun isNetConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            (capabilities != null &&
                    ((capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            (activeNetworkInfo != null && activeNetworkInfo.isConnected)
        }
    }

    fun isNoInternet(context: Context) = !isNetConnected(context)

    fun checkPermission(
        context: Fragment,
        vararg permissions: String,
        reqCode: Int = PERMISSION_REQ_CODE
    ): Boolean {
        var allPermitted = false
        for (permission in permissions) {
            allPermitted = (ContextCompat.checkSelfPermission(context.requireContext(), permission)
                    == PackageManager.PERMISSION_GRANTED)
            if (!allPermitted) break
        }
        if (allPermitted) return true
        context.requestPermissions(
            permissions,
            reqCode
        )
        return false
    }

    fun getGSONObj(): Gson {
        return GsonBuilder().create()
    }

    fun isPermissionOk(vararg results: Int): Boolean {
        var isAllGranted = true
        for (result in results) {
            if (PackageManager.PERMISSION_GRANTED != result) {
                isAllGranted = false
                break
            }
        }
        return isAllGranted
    }

    fun askContactPermission(context: Fragment): Boolean {
        return checkPermission(
            context, Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
    }

    fun isContactPermissionOk(context: Context): Boolean {
        return (ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        )
                == PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CONTACTS
        )
                == PackageManager.PERMISSION_GRANTED)
    }

    fun showLoggedInAlert(
        context: Activity, preference: SharedPreferencesManager,
        db: ChatUserDatabase
    ) {
        try {
            val dialog = Dialog(context)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.alert_dialog)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.findViewById<TextView>(R.id.txt_log_out).setOnClickListener {
                dialog.dismiss()
                UserUtils.logOut(context, preference, db)
            }
            dialog.show()
        } catch (_: Exception) {
        }
    }

    fun setOnlineStatus(
        context: Context,
        txtView: TextView,
        status: UserStatus,
        uId: String
    ) {
        txtView.visibility = View.VISIBLE
        txtView.text = when {
            status.typing_status == UserStatusConstants.TYPING && uId == status.chat_user -> context.getString(
                R.string.typing
            )
            status.status == UserStatusConstants.ONLINE -> context.getString(R.string.online)
            status.last_seen > 0L -> String.format(
                "%s %s",
                context.getString(R.string.last_seen),
                getLastSeen(status.last_seen, context)
            )
            else -> "..."
        }
    }

    fun createBuilder(
        context: Context,
        manager: NotificationManagerCompat,
        isSummary: Boolean = false
    ): NotificationCompat.Builder {
        val channelId = context.packageName
        val notBuilder = NotificationCompat.Builder(context, channelId)
        notBuilder.setSmallIcon(R.drawable.ic_stat_name)
        notBuilder.setAutoCancel(true)
        notBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notBuilder.setDefaults(Notification.DEFAULT_ALL)
        notBuilder.priority = NotificationCompat.PRIORITY_HIGH
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        notBuilder.setSound(soundUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (isSummary) NotificationManager.IMPORTANCE_HIGH else
                NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                channelId, context.getString(R.string.notifications),
                importance
            )
            channel.importance = importance
            channel.shouldShowLights()
            channel.lightColor = Color.BLUE
            channel.canBypassDnd()
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.setSound(soundUri, audioAttributes)
            channel.description = context.getString(R.string.not_description)
            notBuilder.setChannelId(channelId)
            manager.createNotificationChannel(channel)
        }
        return notBuilder
    }

    fun returnNManager(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }

    fun removeNotification(context: Context) {
        val manager = returnNManager(context)
        manager.cancelAll()
    }

    fun removeNotificationById(
        context: Context,
        id: Int
    ) {
        val manager = returnNManager(context)
        manager.cancel(id)
    }

    fun getGroupName(groupId: String) =
        groupId.substring(0, groupId.lastIndexOf("_"))

    fun myMsgStatus(myUserId: String, msg: GroupMessage): Int {
        val indexOfMine = myIndexOfStatus(myUserId, msg)
        return msg.status[indexOfMine]
    }

    fun myIndexOfStatus(myUserId: String, msg: GroupMessage): Int {
        return msg.to.indexOf(myUserId)
    }

    @SuppressLint("SimpleDateFormat")
    fun getTime(
        sentTime: Long,
        context: Context
    ): String {
        val currentTime = System.currentTimeMillis()
        val dayCount = (currentTime - sentTime) / (24 * 60 * 60 * 1000)
        val calender = java.util.Calendar.getInstance()
        calender.timeInMillis = sentTime
        val date = calender.time
        return when {
            dayCount >= 365 -> SimpleDateFormat("MM/yyyy").format(date)
            dayCount in 2..364 -> SimpleDateFormat("dd/MM").format(date)
            dayCount == 1L -> context.getString(R.string.yesterday)
            else -> SimpleDateFormat("hh:mm").format(date)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getLastSeen(
        lastSeen: Long,
        context: Context
    ): String {
        val currentTime = System.currentTimeMillis()
        val dayCount = (currentTime - lastSeen) / (24 * 60 * 60 * 1000)
        val calender = java.util.Calendar.getInstance()
        calender.timeInMillis = lastSeen
        val date = calender.time
        return when {
            dayCount >= 365 -> SimpleDateFormat("MM/yyyy").format(date)
            dayCount > 1L -> SimpleDateFormat("dd/MM").format(date)
            dayCount == 1L -> String.format(
                "%s %s",
                context.getString(R.string.yesterday),
                SimpleDateFormat("hh:mm").format(date)
            )
            else -> String.format(
                "%s %s",
                context.getString(R.string.today),
                SimpleDateFormat("hh:mm").format(date)
            )
        }
    }
}

