package com.khtn.zone.core

import android.content.Context
import com.google.firebase.firestore.CollectionReference
import com.khtn.zone.FirebasePush
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.utils.listener.OnSuccessListener
import com.khtn.zone.utils.UserUtils
import com.khtn.zone.model.UserProfile
import com.khtn.zone.repo.DatabaseRepo
import com.khtn.zone.utils.ContactUtils
import com.khtn.zone.utils.Utils

class ChatUserUtil(
    private val dbRepository: DatabaseRepo,
    private val usersCollection: CollectionReference,
    private val listener: OnSuccessListener?
) {

    fun queryNewUserProfile(
        context: Context, chatUserId: String, docId: String?, unReadCount: Int = 1,
        showNotification: Boolean = false
    ) {
        try {
            usersCollection.document(chatUserId)
                .get().addOnSuccessListener { profile ->
                    if (profile.exists()) {
                        val userProfile = profile.toObject(UserProfile::class.java)
                        val mobile =
                            userProfile?.mobile?.country + " " + userProfile?.mobile?.number
                        val chatUser = ChatUser(userProfile?.uId!!, mobile, userProfile)
                        val isPermissionContact = Utils.isPermissionOk(
                            context = context,
                            permissions = ContactUtils.CONTACT_PERMISSION
                        )

                        chatUser.unRead = unReadCount
                        if (docId != null)
                            chatUser.documentId = docId
                        if (isPermissionContact) {
                            val contacts = UserUtils.fetchContacts(context)
                            val savedContact =
                                contacts.firstOrNull { it.mobile.number.contains(userProfile.mobile!!.number) }
                            savedContact?.let {
                                chatUser.localName = it.name
                                chatUser.locallySaved = true
                            }
                        }
                        listener?.onResult(true, chatUser)
                        dbRepository.insertUser(chatUser)
                        if (showNotification)
                            FirebasePush.showNotification(context, dbRepository)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}