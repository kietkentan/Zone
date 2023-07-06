package com.khtn.zone.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.khtn.zone.R
import com.khtn.zone.TYPE_NEW_GROUP_MESSAGE
import com.khtn.zone.utils.DataConstants.GROUP_DATA
import com.khtn.zone.utils.WorkerConstants.MESSAGE_DATA
import com.khtn.zone.utils.WorkerConstants.MESSAGE_FILE_URI
import com.khtn.zone.core.GroupMessageSender
import com.khtn.zone.core.GroupMsgStatusUpdater
import com.khtn.zone.core.OnGrpMessageResponse
import com.khtn.zone.database.dao.ChatUserDao
import com.khtn.zone.database.dao.GroupMessageDao
import com.khtn.zone.database.data.Group
import com.khtn.zone.database.data.GroupMessage
import com.khtn.zone.di.GroupCollection
import com.khtn.zone.model.SetSticker
import com.khtn.zone.model.Sticker
import com.khtn.zone.repo.DatabaseRepo
import com.khtn.zone.service.GroupUploadWorker
import com.khtn.zone.utils.FireStoreCollection
import com.khtn.zone.utils.MessageTypeConstants
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.UserUtils
import com.khtn.zone.utils.printMeD
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preference: SharedPreferencesManager,
    private val groupMsgDao: GroupMessageDao,
    private val chatUserDao: ChatUserDao,
    private val dbRepository: DatabaseRepo,
    private val groupMsgStatusUpdater: GroupMsgStatusUpdater,
    private val firebaseFireStore: FirebaseFirestore,
    @GroupCollection
    private val groupCollection: CollectionReference
): ViewModel() {
    val message = MutableLiveData<String>()
    val typingUsers = MutableLiveData<String>()
    private val currentGroup = preference.getOnlineGroup()
    private val fromUser = preference.getUid()
    private var isTyping = false
    private var groupListener: ListenerRegistration? = null
    private val typingHandler = Handler(Looper.getMainLooper())
    private var canScroll = false
    private var cleared = false

    private lateinit var group: Group

    private val _listSetSticker: MutableLiveData<List<SetSticker>?> = MutableLiveData()
    val listSetSticker: LiveData<List<SetSticker>?>
        get() = _listSetSticker

    private val _listAllSticker: MutableLiveData<List<String>> = MutableLiveData()
    val listAllSticker: LiveData<List<String>>
        get() = _listAllSticker

    private val _allSticker: MutableLiveData<Map<SetSticker, List<Sticker>>> = MutableLiveData()
    val allSticker: LiveData<Map<SetSticker, List<Sticker>>>
        get() = _allSticker

    init {
        groupCollection.document(currentGroup).addSnapshotListener { value, error ->
            try {
                if (error == null) {
                    val list = value?.get("typing_users")
                    val users = if (list == null) ArrayList()
                    else list as ArrayList<String>
                    val names = group.members?.filter { users.contains(it.id) && it.id != fromUser }
                        ?.map {
                            // get locally saved name
                            "${it.localName} ${context.getString(R.string.is_typing)}"
                        }
                    if (users.isEmpty())
                        typingUsers.postValue("")
                    else
                        typingUsers.postValue(TextUtils.join(", ", names!!))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getGroupMessages(groupId: String) = groupMsgDao.getChatsOfGroup(groupId)

    fun getChatUsers() = chatUserDao.getAllChatUser()

    fun setGroup(group: Group) {
        if (!this::group.isInitialized) {
            this.group = group
            setSeenAllMessage()
        }
    }

    fun setSeenAllMessage() {
        if (this::group.isInitialized) {
            group.unRead = 0
            dbRepository.insertGroup(group)
            viewModelScope.launch(Dispatchers.IO) {
                val messageList = dbRepository.getChatsOfGroupList(group.id)
                withContext(Dispatchers.Main) {
                    groupMsgStatusUpdater.updateToSeen(fromUser!!, messageList, group.id)
                }
            }
        }
    }

    fun canScroll(can: Boolean) {
        canScroll = can
    }

    fun getCanScroll() = canScroll

    fun sendTyping(edtValue: String) {
        if (edtValue.isEmpty()) {
            if (isTyping)
                sendTypingStatus(false, fromUser!!, currentGroup)
            isTyping = false
        } else if (!isTyping) {
            sendTypingStatus(true, fromUser!!, currentGroup)
            isTyping = true
            removeTypingCallbacks()
            typingHandler.postDelayed(typingThread, 4000)
        }
    }

    private fun sendTypingStatus(
        isTyping: Boolean,
        fromUser: String, currentGroup: String
    ) {
        val value =
            if (isTyping) FieldValue.arrayUnion(fromUser) else FieldValue.arrayRemove(fromUser)
        groupCollection.document(currentGroup).update("typing_users", value)
    }

    private val typingThread = Runnable {
        isTyping = false
        sendTypingStatus(false, fromUser!!, currentGroup)
        removeTypingCallbacks()
    }

    private fun removeTypingCallbacks() {
        typingHandler.removeCallbacks(typingThread)
    }

    fun sendCachedTxtMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            updateCacheMessges(groupMsgDao.getChatsOfGroupList(currentGroup))
        }
    }

    private suspend fun updateCacheMessges(chatsOfGroup: List<GroupMessage>) {
        withContext(Dispatchers.Main) {
            val nonSendMsgs = chatsOfGroup.filter {
                it.from == fromUser
                        && it.status[0] == 0 && it.type == MessageTypeConstants.TEXT
            }
            "nonSendMsgs Group Size ${nonSendMsgs.size}".printMeD()
            for (cachedMsg in nonSendMsgs) {
                val messageSender = GroupMessageSender(groupCollection)
                messageSender.sendMessage(cachedMsg, group, messageListener)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleared = true
        groupListener?.remove()
    }

    fun sendMessage(message: GroupMessage) {
        Handler(Looper.getMainLooper()).postDelayed({
            val messageSender = GroupMessageSender(groupCollection)
            messageSender.sendMessage(message, group, messageListener)
        }, 300)
        UserUtils.insertGroupMsg(groupMsgDao, message)
    }

    fun uploadToCloud(message: GroupMessage, fileUri: String) {
        try {
            UserUtils.insertGroupMsg(groupMsgDao, message)
            removeTypingCallbacks()
            val messageData = Json.encodeToString(message)
            val groupData = Json.encodeToString(group)
            val data = Data.Builder()
                .putString(MESSAGE_FILE_URI, fileUri)
                .putString(MESSAGE_DATA, messageData)
                .putString(GROUP_DATA, groupData)
                .build()
            val uploadWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<GroupUploadWorker>()
                    .setInputData(data)
                    .build()
            WorkManager.getInstance(context).enqueue(uploadWorkRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private val messageListener = object : OnGrpMessageResponse {
        override fun onSuccess(message: GroupMessage) {
            "messageListener OnSuccess ${message.textMessage?.text}".printMeD()
            UserUtils.insertGroupMsg(groupMsgDao, message)
            val users = group.members?.filter { it.user.token.isNotEmpty() }?.map {
                it.user.token
                it
            }
            users?.forEach {
                UserUtils.sendPush(
                    context,
                    TYPE_NEW_GROUP_MESSAGE,
                    Json.encodeToString(message),
                    it.user.token,
                    it.id
                )
            }
        }

        override fun onFailed(message: GroupMessage) {
            "messageListener onFailed ${message.createdAt}".printMeD()
            UserUtils.insertGroupMsg(groupMsgDao, message)
        }
    }

    fun getSetSticker(list: List<String>) {
        firebaseFireStore.collection(FireStoreCollection.STICKER)
            .whereIn("id", list)
            .get()
            .addOnSuccessListener {
                val listSet: MutableList<SetSticker> = arrayListOf()

                for (document in it) {
                    val set = document.toObject(SetSticker::class.java)
                    listSet.add(set)
                }
                _listSetSticker.postValue(listSet)
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun getAllListSticker(list: List<String>) {
        val st: MutableList<String> = mutableListOf()

        firebaseFireStore.collection(FireStoreCollection.STICKER)
            .whereIn("id", list)
            .get()
            .addOnSuccessListener {
                for (document in it) {
                    val sticker = document.get(FireStoreCollection.LIST_STICKER)
                    val id = document.get(FireStoreCollection.ID)
                    if (sticker != null && id != null) {
                        val lSticker = sticker as List<String>
                        st.addAll(lSticker)
                    }
                }
                _listAllSticker.value = st
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

    fun getAllSticker(list: List<String>, set: List<SetSticker>) {
        val map: HashMap<SetSticker, MutableList<Sticker>> = hashMapOf()

        firebaseFireStore.collection(FireStoreCollection.STICKER_ITEM)
            .whereIn("id", list)
            .get()
            .addOnSuccessListener {
                for (document in it) {
                    val sticker = document.toObject<Sticker>()
                    val setSticker = set.first {it -> it.id == sticker.setStickerId}
                    if (map[setSticker] == null)
                        map[setSticker] = mutableListOf()
                    map[setSticker]!!.add(sticker)
                }
                _allSticker.value = map
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }
}