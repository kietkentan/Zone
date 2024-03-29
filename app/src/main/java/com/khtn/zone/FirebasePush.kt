package com.khtn.zone

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.khtn.zone.core.ChatUserUtil
import com.khtn.zone.core.GroupMsgStatusUpdater
import com.khtn.zone.core.GroupQuery
import com.khtn.zone.core.MessageStatusUpdater
import com.khtn.zone.database.data.*
import com.khtn.zone.di.GroupCollection
import com.khtn.zone.di.UserCollection
import com.khtn.zone.utils.*
import com.khtn.zone.utils.listener.OnSuccessListener
import com.khtn.zone.model.PushMessage
import com.khtn.zone.repo.DatabaseRepo
import com.khtn.zone.utils.NotificationUtils
import com.khtn.zone.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

const val TYPE_LOGGED_IN = "new_logged_in"
const val TYPE_NEW_MESSAGE = "new_message"
const val TYPE_NEW_GROUP = "new_group"
const val TYPE_NEW_GROUP_MESSAGE = "new_group_message"
const val GROUP_KEY = "com.mygroupkey"
const val SUMMARY_ID = 0
const val KEY_TEXT_REPLY = "key_text_reply"

@AndroidEntryPoint
class FirebasePush: FirebaseMessagingService(), OnSuccessListener {

    @Inject
    lateinit var preference: SharedPreferencesManager

    @Inject
    lateinit var dbRepository: DatabaseRepo

    @UserCollection
    @Inject
    lateinit var usersCollection: CollectionReference

    @Inject
    lateinit var messageStatusUpdater: MessageStatusUpdater

    @Inject
    lateinit var groupMessageStatusUpdater: GroupMsgStatusUpdater

    @GroupCollection
    @Inject
    lateinit var groupCollection: CollectionReference

    private var sentTime: Long? = null
    private lateinit var pushMsg: PushMessage
    private var userId: String? = null
    private lateinit var messagesOfChatUser: List<Message>

    override fun onCreate() {
        super.onCreate()
        userId = preference.retrieveStringByKey(SharedPrefConstants.UID).toString()
    }

    override fun onNewToken(token: String) {
        "Token: $token".printMeD()
        preference.saveStringByKey(SharedPrefConstants.TOKEN, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        "Re: ${remoteMessage.notification?.body}".printMeD()

        try {
            if (!preference.retrieveBooleanByKey(SharedPrefConstants.LOGIN) ||
                !preference.retrieveBooleanByKey(SharedPrefConstants.LAST_LOGGED_DEVICE_SAME, true))
                return
            sentTime = remoteMessage.sentTime
            val data = remoteMessage.data
            pushMsg = Json.decodeFromString(data["data"].toString())
            pushMsg.to?.let {
                "UserId = $it".printMeD()
                if (it != userId)
                    return
            }
            handleNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNotification() {
        "Type: ${pushMsg.type}".printMeD()
        when (pushMsg.type) {
            TYPE_LOGGED_IN -> {
                preference.saveBooleanByKey(SharedPrefConstants.LAST_LOGGED_DEVICE_SAME, false)
                val intent = Intent(ActionConstants.ACTION_LOGGED_IN_ANOTHER_DEVICE)
                sendBroadcast(intent)
            }

            TYPE_NEW_MESSAGE -> { handleNewMessage() }

            TYPE_NEW_GROUP -> { handleNewGroup() }

            TYPE_NEW_GROUP_MESSAGE -> { handleGroupMsg() }

            else -> { /*Any*/ }
        }
    }

    private fun handleGroupMsg() {
        //it would be updated by snapshot listeners when app is alive
        val message = Json.decodeFromString<GroupMessage>(pushMsg.message_body.toString())
        CoroutineScope(Dispatchers.IO).launch {
            dbRepository.insertMessage(message)
            val group = dbRepository.getGroupById(message.groupId)
            val messages = dbRepository.getChatsOfGroupList(group?.id.toString())
            if (group != null) {
                group.unRead = messages.filter {
                    it.from != userId &&
                            Utils.myIndexOfStatus(userId!!, it) < 3
                }.size
                dbRepository.insertGroup(group)

                withContext(Dispatchers.Main) {
                    showGroupNotification(this@FirebasePush, dbRepository)
                    //update delivery status
                    groupMessageStatusUpdater.updateToDelivery(userId!!, messages, group.id)
                }
            } else {
                val groupQuery = GroupQuery(message.groupId, dbRepository, preference)
                groupQuery.getGroupData(groupCollection)
            }

        }
    }

    private fun handleNewGroup() {
        //it would be updated by snapshot listeners when app is alive
        val group = Json.decodeFromString<Group>(pushMsg.message_body.toString())
        val groupQuery = GroupQuery(group.id, dbRepository, preference)
        groupQuery.getGroupData(groupCollection)

    }

    private fun handleNewMessage() {
        val message = Json.decodeFromString<Message>(pushMsg.message_body.toString())
        if (message.to != userId) {
            "notSame".printMeD()
            Timber.v("Push notification ignored")
            return
        }
        val chatUserId = UserUtils.getChatUserId(userId!!, message)  // chatUserId from message
        message.chatUserId = chatUserId
        CoroutineScope(Dispatchers.IO).launch {
            dbRepository.insertMessage(message)
            val chatUser = dbRepository.getChatUserById(chatUserId)
            messagesOfChatUser = dbRepository.getChatsOfFriend(chatUserId)
                .filter { it.to == userId && it.status < 3 }
            if (chatUser != null) {
                chatUser.unRead = messagesOfChatUser.size  // set unread msg count
                dbRepository.insertUser(chatUser)
                withContext(Dispatchers.Main) {
                    showNotification(this@FirebasePush, dbRepository)
                    // update delivery status
                    messageStatusUpdater.updateToDelivery(messagesOfChatUser, chatUser)
                }
            } else {
                withContext(Dispatchers.Main) {
                    // update delivery status in listener
                    val util = ChatUserUtil(dbRepository, usersCollection, this@FirebasePush)
                    util.queryNewUserProfile(
                        this@FirebasePush,
                        chatUserId,
                        null,
                        showNotification = true
                    )
                }
            }
        }
    }

    private suspend fun getBitmap(url: String): Bitmap {
        val request = Glide.with(this)
            .asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .submit()
        val bitmap: Bitmap = withContext(Dispatchers.IO) {
            request.get()
        }
        return bitmap
    }

    companion object {
        //notification method for common use
        var messageCount = 0
        var personCount = 0

        fun showGroupNotification(context: Context, dbRepository: DatabaseRepo) {
            CoroutineScope(Dispatchers.IO).launch {
                var groupWithMsgs = dbRepository.getGroupWithMessagesList()
                groupWithMsgs = groupWithMsgs.filter { it.group.unRead != 0 }
                checkGroupMessages(context, groupWithMsgs)
            }
        }

        fun showNotification(context: Context, dbRepository: DatabaseRepo) {
            CoroutineScope(Dispatchers.IO).launch {
                var chatUserWithMessages = dbRepository.getChatUserWithMessagesList()
                chatUserWithMessages = chatUserWithMessages.filter { it.user.unRead != 0 }
                checkMessages(context, chatUserWithMessages)
            }
        }

        @SuppressLint("MissingPermission")
        private fun checkGroupMessages(
            context: Context,
            groupWithMsgs: List<GroupWithMessages>
        ) {
            messageCount = 0
            personCount = 0
            val myUserId = SharedPreferencesManager(context).retrieveStringByKey(SharedPrefConstants.UID).toString()
            val manager: NotificationManagerCompat = Utils.returnNManager(context)
            val groupNotifications = ArrayList<Notification>()
            if (groupWithMsgs.isNotEmpty()) {
                for (groupMsg in groupWithMsgs) {
                    if (groupMsg.messages.last().from == myUserId)
                          continue
                    personCount += 1
                    val person: Person = Person.Builder().setIcon(null)
                        .setKey(groupMsg.group.id).setName(Utils.getGroupName(groupMsg.group.id))
                        .build()
                    val builder = Utils.createBuilder(context, manager)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setStyle(NotificationUtils.getGroupStyle(context, myUserId, person, groupMsg))
                        .setContentIntent(NotificationUtils.getGroupMsgIntent(context, groupMsg.group))
                        .setGroup(GROUP_KEY)
                    builder.addAction(
                        R.drawable.ic_drafts,
                        "mark as read",
                        NotificationUtils.getGroupMarkAsPIntent(context, groupMsg)
                    )
                    builder.addAction(NotificationUtils.getGroupReplyAction(context, groupMsg))
                    val notification = builder.build()
                    groupNotifications.add(notification)
                }
            }

            val summaryNotification = NotificationUtils.getSummaryNotification(context, manager)
            for ((index, notification) in groupNotifications.withIndex()) {
                val notIdString = groupWithMsgs[index].group.createdAt.toString()
                val notId = notIdString.substring(notIdString.length - 4).toInt() //last 4 digits as notificationId
                manager.notify(notId, notification)
            }
            if (groupNotifications.size > 1)
                manager.notify(SUMMARY_ID, summaryNotification)
        }

        @SuppressLint("MissingPermission")
        private fun checkMessages(
            context: Context,
            chatUserWithMessages: List<ChatUserWithMessages>
        ) {
            if (chatUserWithMessages.isEmpty())
                return

            messageCount = 0
            personCount = 0
            val notifications = ArrayList<Notification>()
            val myUserId =
                SharedPreferencesManager(context).retrieveStringByKey(SharedPrefConstants.UID).toString()
            val manager: NotificationManagerCompat = Utils.returnNManager(context)

            for (user in chatUserWithMessages) {
                val messages = user.messages.filter { it.status < 3 && it.from != myUserId }
                if (messages.isEmpty()) {
                    "isEmpty".printMeD()
                    continue
                }
                personCount += 1
                Timber.v("DocId ${user.user.documentId}")
                val person: Person = Person.Builder().setIcon(null)
                    .setKey(user.user.id).setName(user.user.localName).build()
                val builder = Utils.createBuilder(context, manager)
                    .setStyle(NotificationUtils.getStyle(context, person, user))
                    .setContentIntent(NotificationUtils.getPIntent(context, user.user))
                    .setGroup(GROUP_KEY)
                if (!user.user.documentId.isNullOrBlank()) {
                    builder.addAction(
                        R.drawable.ic_drafts,
                        context.getString(R.string.mark_as_read),
                        NotificationUtils.getMarkAsPIntent(context, user)
                    )
                    builder.addAction(NotificationUtils.getReplyAction(context, user))
                }
                val notification = builder.build()
                notifications.add(notification)
            }

            val summaryNotification = NotificationUtils.getSummaryNotification(context, manager)
            for ((index, notification) in notifications.withIndex()) {
                val notiIdString = chatUserWithMessages[index].user.user.createdAt.toString()
                // last 4 digits as notificationId
                val notiId = notiIdString.substring(notiIdString.length - 4).toInt()
                manager.notify(notiId, notification)
            }

            if (notifications.size > 1)
                manager.notify(SUMMARY_ID, summaryNotification)
        }
    }

    override fun onResult(
        success: Boolean,
        data: Any?
    ) {
        if (success) {
            messageStatusUpdater.updateToDelivery(messagesOfChatUser, data as ChatUser)
        }
    }
}