package com.khtn.zone.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.khtn.zone.FirebasePush
import com.khtn.zone.GROUP_KEY
import com.khtn.zone.KEY_TEXT_REPLY
import com.khtn.zone.R
import com.khtn.zone.activity.MainActivity
import com.khtn.zone.database.data.*

object NotificationUtils {
    fun getSummaryNotification(
        context: Context,
        manager: NotificationManagerCompat
    ): Notification {
        return Utils.createBuilder(context, manager, true)
            .setContentTitle("Summary")
            .setContentText(String.format(context.getString(R.string.new_messages), FirebasePush.messageCount))
            .setSmallIcon(R.drawable.ic_stat_name)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle(String.format(context.getString(R.string.new_messages), FirebasePush.messageCount))
                    .setSummaryText(String.format(context.getString(R.string.new_messages_from), FirebasePush.messageCount, FirebasePush.personCount))
            )
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setGroup(GROUP_KEY)
            .setContentIntent(getHomeIntent(context))
            .setGroupSummary(true)
            .build()
    }

    fun getReplyAction(
        context: Context,
        user: ChatUserWithMessages
    ): NotificationCompat.Action {
        val replyLabel = context.getString(R.string.reply)
        val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel(replyLabel)
            build()
        }
        return NotificationCompat.Action.Builder(
            R.drawable.ic_send,
            replyLabel,
            getReplyPIntent(context, user)
        )
            .addRemoteInput(remoteInput)
            .build()
    }

    fun getGroupReplyAction(
        context: Context,
        user: GroupWithMessages
    ): NotificationCompat.Action {
        val replyLabel = context.getString(R.string.reply)
        val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel(replyLabel)
            build()
        }
        return NotificationCompat.Action.Builder(
            R.drawable.ic_send,
            replyLabel,
            getGroupReplyPIntent(context, user)
        )
            .addRemoteInput(remoteInput)
            .build()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getHomeIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun getMarkAsPIntent(
        context: Context,
        user: ChatUserWithMessages
    ): PendingIntent {
        val snoozeIntent = Intent(context, NActionReceiver::class.java).apply {
            putExtra(DataConstants.CHAT_DATA, user)
            action = ActionConstants.ACTION_MARK_AS_READ
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getReplyPIntent(
        context: Context,
        user: ChatUserWithMessages
    ): PendingIntent {
        val intent = Intent(context, NActionReceiver::class.java).apply {
            putExtra(DataConstants.CHAT_DATA, user)
            action = ActionConstants.ACTION_REPLY
        }

        return PendingIntent.getBroadcast(
            context,
            5,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun getGroupReplyPIntent(
        context: Context,
        user: GroupWithMessages
    ): PendingIntent {
        val intent = Intent(context, GroupMsgActionReceiver::class.java).apply {
            putExtra(DataConstants.GROUP_DATA, user)
            action = ActionConstants.ACTION_REPLY
        }
        return PendingIntent.getBroadcast(
            context,
            6,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun getGroupMarkAsPIntent(
        context: Context,
        user: GroupWithMessages
    ): PendingIntent {
        val snoozeIntent = Intent(context, GroupMsgActionReceiver::class.java).apply {
            putExtra(DataConstants.GROUP_DATA, user)
            action = ActionConstants.ACTION_MARK_AS_READ
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun getPIntent(context: Context, user: ChatUser): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.action = ActionConstants.ACTION_NEW_MESSAGE
        intent.putExtra(DataConstants.CHAT_USER_DATA, user)
        intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_ONE_SHOT or
                    PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun getGroupMsgIntent(
        context: Context,
        group: Group
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.action = ActionConstants.ACTION_GROUP_NEW_MESSAGE
        intent.putExtra(DataConstants.GROUP_DATA, group)
        intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_ONE_SHOT or
                    PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun getStyle(
        context: Context,
        person: Person,
        user: ChatUserWithMessages
    ): NotificationCompat.Style {
        val style = NotificationCompat.MessagingStyle(person)
        val chatPerson: Person = Person.Builder().setIcon(null)
            .setKey(user.user.id).setName(user.user.localName).build()
        val messages =
            user.messages.filter { it.status < 3 && it.from != SharedPreferencesManager(context).retrieveStringByKey(SharedPrefConstants.UID) }
        for (message in messages) {
            FirebasePush.messageCount += 1
            style.addMessage(BindingAdapters.getLastMsgTxt(message), message.createdAt, chatPerson)
        }
        return style
    }

    fun getGroupStyle(
        context: Context,
        myUserId: String,
        person: Person,
        group: GroupWithMessages
    ): NotificationCompat.Style {
        val style = NotificationCompat.MessagingStyle(person)
        val members = group.group.members ?: ArrayList()
        val messages = group.messages
        val filterMessages = messages.filter {
            it.from != myUserId &&
                    Utils.myMsgStatus(myUserId, it) < 3
        }
        if (filterMessages.isNotEmpty()) {
            for (msg in filterMessages)
                style.addMessage(getGroupMsg(members, msg), msg.createdAt, person)
            FirebasePush.messageCount += 1
        }
        return style
    }

    private fun getGroupMsg(
        members: ArrayList<ChatUser>,
        msg: GroupMessage
    ): String {
        val user = members.first { it.id == msg.from }.localName
        return "$user : ${BindingAdapters.getLastMsgTxt(msg)}"
    }
}