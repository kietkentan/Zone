@file:Suppress("DEPRECATION")

package com.khtn.zone.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import com.fcmsender.FCMSender
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.khtn.zone.MyApplication
import com.khtn.zone.activity.SplashScreenActivity
import com.khtn.zone.adapter.GroupChatHomeAdapter
import com.khtn.zone.adapter.SingleChatHomeAdapter
import com.khtn.zone.core.*
import com.khtn.zone.database.ChatUserDatabase
import com.khtn.zone.database.dao.GroupDao
import com.khtn.zone.database.dao.GroupMessageDao
import com.khtn.zone.database.data.*
import com.khtn.zone.model.Contact
import com.khtn.zone.model.ModelDeviceDetails
import com.khtn.zone.model.ModelMobile
import com.khtn.zone.model.UserProfile
import com.khtn.zone.model.UserStatus
import com.khtn.zone.repo.DatabaseRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.system.measureNanoTime

object UserUtils {
    const val NOTIFICATION_ID = 22

    var queriedList = ArrayList<UserProfile>()
    var resultCount = 0
    var totalRecursionCount = 0

    fun updatePushToken(
        context: Context,
        userCollection: CollectionReference,
        isSave: Boolean
    ) {
        try {
            if (Utils.isNetConnected(context)) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        task.exception?.printStackTrace()
                        return@OnCompleteListener
                    }

                    SharedPreferencesManager(context = context).saveStringByKey(
                        SharedPrefConstants.TOKEN,
                        task.result
                    )
                    if (isSave)
                        updateDeviceDetails(context, userCollection)
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("LogNotTimber")
    private fun updateDeviceDetails(
        context: Context,
        userCollection: CollectionReference
    ) {
        val preference = SharedPreferencesManager(context = context)
        val token = preference.retrieveStringByKey(SharedPrefConstants.TOKEN)
        Timber.v("AAA ${preference.getUid()}")
        Timber.v("BB ${preference.getUserProfile()?.uId}")
        if (token.isNullOrEmpty())
            updatePushToken(context, userCollection, true)
        else if (Utils.isNetConnected(context)) {
            val profile = preference.getUserProfile()?.apply {
                this.token = token
                this.deviceDetails =
                    Json.decodeFromString<ModelDeviceDetails>(getDeviceInfo(context).toString())
            }
            val updateData = hashMapOf(
                "token" to token,
                "updatedAt" to System.currentTimeMillis(),
                "device_details" to Json.decodeFromString<ModelDeviceDetails>(getDeviceInfo(context).toString()),
            )
            userCollection.document(preference.retrieveStringByKey(SharedPrefConstants.UID)!!)
                .update(updateData).addOnSuccessListener {
                preference.saveUserProfile(profile!!)
                "Token Updated $token##".printMeD()
            }
        }
    }

    fun getStorageRef(context: Context): StorageReference {
        val ref = Firebase.storage.getReference(FirebaseStorageConstants.USER)
        return ref.child(SharedPreferencesManager(context = context).retrieveStringByKey(SharedPrefConstants.UID).toString())
    }

    fun getDocumentRef(context: Context): DocumentReference {
        val db = FirebaseFirestore.getInstance()
        return db.collection(FireStoreCollection.USER)
            .document(SharedPreferencesManager(context = context).getUid()!!)
    }

    fun getMessageSubCollectionRef(): Query {
        val db = FirebaseFirestore.getInstance()
        return db.collectionGroup(FireStoreCollection.MESSAGE)
    }

    fun getGroupMsgSubCollectionRef(): Query {
        val db = FirebaseFirestore.getInstance()
        return db.collectionGroup(FireStoreCollection.GROUP_MESSAGE)
    }

    @SuppressLint("Range")
    fun fetchContacts(context: Context): List<Contact> {
        val names = ArrayList<String>()
        val numbers = ArrayList<String>()
        val contacts = ArrayList<Contact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection: String? =
            null //it's like a where concept in mysql
        val selectionArgs: Array<String>? = null
        val sortOrder: String? = null
        val resolver = context.contentResolver
        val cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder)
        while (cursor!!.moveToNext()) {
            val name =
                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val number =
                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            if (number.contains(SharedPreferencesManager(context = context).getMobile()!!.number))
                continue
            names.add(name)
            numbers.add(number)
            contacts.add(Contact(name, ModelMobile(number = number)))
        }
        cursor.close()
        val hashMap = getCountryCodeRemovedList(context, contacts)
        contacts.clear()
        for (number in hashMap.keys) {
            contacts.add(Contact(hashMap[number].toString(), ModelMobile(number = number)))
        }
        return contacts.sortedWith(compareBy { it.name })
    }

    private fun getCountryCodeRemovedList(
        context: Context,
        contacts: ArrayList<Contact>
    ): HashMap<String, String> {
        val hashMap: HashMap<String, String> = HashMap() //hashmap to get rid of duplication
        contacts.forEach { contact ->
            if (contact.mobile.number.length <= 5 ||
                contact.mobile.number.contains(SharedPreferencesManager(context = context).getMobile()?.number!!)
            )
                return@forEach
            val mobile = contact.mobile
            /*for (country in Countries.getCountries()) {
                if (mobile.contains(country.noCode)) {
                    mobile = contact.mobile.replace(country.noCode, "")
                    break
                }
            }*/
            hashMap[mobile.number.replace(" ", "")] = contact.name
        }
        return hashMap
    }

    fun getDeviceInfo(context: Context): JSONObject {
        try {
            val deviceInfo = JSONObject()
            deviceInfo.put("device_id", getDeviceId(context))
            deviceInfo.put("device_model", Build.MODEL)
            deviceInfo.put("device_brand", Build.BOARD)
            deviceInfo.put("device_country", Locale.getDefault())
            deviceInfo.put("device_os_v", Build.VERSION.RELEASE)
            deviceInfo.put("app_version", getVersionName(context))
            deviceInfo.put("package_name", context.packageName)
            deviceInfo.put("device_type", "android")
            return deviceInfo
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return JSONObject()
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String? {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    private fun getVersionName(context: Context): String? {
        try {
            val packageName = context.packageName
            val pInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PERMISSION_GRANTED
            )
            return pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return "1.0"
    }

    fun logOut(
        context: Activity,
        preference: SharedPreferencesManager,
        db: ChatUserDatabase
    ) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                db.clearAllTables()
            }
            EventBus.getDefault().post(UserStatus(UserStatusConstants.OFFLINE))
            ChatHandler.removeListeners()
            GroupChatHandler.removeListener()
            ChatUserProfileListener.removeListener()
            SingleChatHomeAdapter.allChatList = emptyList<ChatUserWithMessages>().toMutableList()
            GroupChatHomeAdapter.allList = emptyList<GroupWithMessages>().toMutableList()
            FirebaseAuth.getInstance().signOut()
            preference.clearData()

            val intent = Intent(context, SplashScreenActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            context.finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("LogNotTimber")
    fun sendPush(
        context: Context,
        type: String,
        body: String,
        token: String,
        to: String
    ) {
        try {
            val data = JSONObject()
            val pushData = JSONObject()
            data.put("type", type)
            data.put("message_body", body)
            data.put("to", to)
            pushData.put("data", data)
            val push = FCMSender.Builder()
                .serverKey(FirebaseCloudMessagingConstants.FCM_SERVER_KEY)
                .setData(pushData)
                .toTokenOrTopic(token)
                .responseListener(object : FCMSender.ResponseListener {
                    override fun onFailure(errorCode: Int, message: String) {
                        "notification sent Failed to $token".printMeD()
                    }

                    override fun onSuccess(response: String) {
                        "notification sent Successfully to $token".printMeD()
                    }
                }).build()
            push.sendPush(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setUnReadCountZero(
        repo: DatabaseRepo,
        chatUser: ChatUser
    ) {
        try {
            val time = measureNanoTime {
                chatUser.unRead = 0
                repo.insertUser(chatUser)
            }
            Timber.v("Taken time $time")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getChatUserId(
        fromUser: String,
        message: Message
    ) = if (message.from != fromUser) message.from
    else message.to

    fun sendTypingStatus(
        database: FirebaseDatabase,
        isTyping: Boolean,
        vararg users: String
    ) {
        try {
            val typingRef = database.getReference("/${FireStoreCollection.USER}/${users[0]}/${FireStoreCollection.TYPING_STATUS}")
            val chatUserRef = database.getReference("/${FireStoreCollection.USER}/${users[0]}/${FireStoreCollection.M_CHAT_USER}")
            typingRef.setValue(if (isTyping) UserStatusConstants.TYPING else UserStatusConstants.NOT_TYPING)
            chatUserRef.setValue(users[1])
            typingRef.onDisconnect().setValue(UserStatusConstants.NON_TYPING)
            chatUserRef.onDisconnect().setValue("")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateContactsProfiles(listener: QueryCompleteListener?): Boolean {
        Timber.v("Query Called")
        val allContacts = fetchContacts(MyApplication.appContext).toMutableList()
        val listOfMobiles = ArrayList<String>()
        allContacts.forEach {
            listOfMobiles.add(it.mobile.number)
        }
        if (listOfMobiles.isEmpty())
            return false
        return makeQueryRecursively(listOfMobiles, 1, listener ?: onQueryCompleted)
    }

    @SuppressLint("LogNotTimber")
    private tailrec fun makeQueryRecursively(
        listOfMobNos: ArrayList<String>,
        position: Int,
        listener: QueryCompleteListener
    ): Boolean {
        val firstTen = ArrayList<String>()
        val size = if (listOfMobNos.size < 10) listOfMobNos.size else 10
        for (index in 0 until size)
            firstTen.add(listOfMobNos[index])
        listOfMobNos.subList(0, size).clear()  //remove queried elements
        val contactsQuery = ContactsQuery(firstTen, position, listener)
        contactsQuery.makeQuery()

        return if (listOfMobNos.isEmpty()) {
            totalRecursionCount = position
            //Log.i(TAG.INFO, "Queried times $position")
            true
        } else makeQueryRecursively(listOfMobNos, position + 1, listener)
    }

    fun getChatUser(
        doc: UserProfile,
        chatUsers: List<ChatUser>,
        savedName: String
    ): ChatUser {
        var existData: ChatUser? = null
        if (chatUsers.isNotEmpty()) {
            val contact = chatUsers.firstOrNull { it.id == doc.uId }
            contact?.let {
                existData = it
            }
        }
        return existData?.apply {
            user = doc
            localName = savedName
            locallySaved = true
        } ?: ChatUser(doc.uId.toString(), savedName, doc, locallySaved = true)
    }


    private val onQueryCompleted = object : QueryCompleteListener {
        override fun onQueryCompleted(queriedList: ArrayList<UserProfile>) {
            try {
                Timber.v("onQueryCompleted ${queriedList.size}")
                val localContacts = fetchContacts(MyApplication.appContext)
                val finalList = ArrayList<ChatUser>()
                // set local saved name to queried users
                CoroutineScope(Dispatchers.IO).launch {
                    val chatUsers = MyApplication.userDaoo.getChatUserList()
                    withContext(Dispatchers.Main) {
                        for (doc in queriedList) {
                            val savedNumber =
                                localContacts.firstOrNull { it.mobile.number == doc.mobile?.number }
                            if (savedNumber != null) {
                                val chatUser = getChatUser(doc, chatUsers, savedNumber.name)
                                finalList.add(chatUser)
                            }
                        }
                        setDefaultValues()
                        withContext(Dispatchers.IO) {
                            MyApplication.userDaoo.insertMultipleUser(finalList)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setDefaultValues() {
        totalRecursionCount = 0
        resultCount = 0
        queriedList.clear()
    }

    fun setUnReadCountGroup(
        groupDao: GroupDao,
        group: Group
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            group.unRead = 0
            groupDao.insertGroup(group)
        }
    }

    fun insertGroupMsg(
        groupMsgDao: GroupMessageDao,
        message: GroupMessage
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            groupMsgDao.insertMessage(message)
        }
    }

    fun insertMultipleGroupMsg(
        groupMsgDao: GroupMessageDao,
        messages: List<GroupMessage>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            groupMsgDao.insertMultipleMessage(messages)
        }
    }
}