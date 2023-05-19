package com.khtn.zone.core

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.database.data.Message
import com.khtn.zone.repo.DatabaseRepo
import com.khtn.zone.utils.FireStoreCollection
import timber.log.Timber

interface OnMessageResponse {
    fun onSuccess(message: Message)
    fun onFailed(message: Message)
}

class MessageSender(
    private val msgCollection: CollectionReference,
    private val dbRepo: DatabaseRepo,
    private val chatUser: ChatUser,
    private val listener: OnMessageResponse
) {

    @SuppressLint("LogNotTimber")
    fun checkAndSend(
        fromUser: String,
        toUser: String,
        message: Message
    ) {
        val docId = chatUser.documentId
        if (!docId.isNullOrEmpty()) {
            Timber.v("Case 0 ${chatUser.documentId}")
            send(docId, message)
        } else {
            // so we don't create multiple nodes for same chat
            msgCollection.document("${fromUser}_${toUser}").get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // this node exists send your message
                        Timber.v("Case 1")
                        send("${fromUser}_${toUser}", message)
                    } else {
                        // senderId_receiverId node doesn't exist check receiverId_senderId
                        msgCollection.document("${toUser}_${fromUser}").get()
                            .addOnSuccessListener { documentSnapshot2 ->
                                if (documentSnapshot2.exists()) {
                                    Timber.v("Case 2")
                                    send("${toUser}_${fromUser}", message)
                                } else {
                                    // no previous chat history(senderId_receiverId & receiverId_senderId both don't exist)
                                    // so we create document senderId_receiverId then messages array then add messageMap to messages
                                    // this node exists send your message
                                    // add ids of chat members
                                    Timber.v("Case 3")
                                    msgCollection.document("${fromUser}_${toUser}")
                                        .set(
                                            mapOf("chat_members" to FieldValue.arrayUnion(fromUser, toUser)),
                                            SetOptions.merge()
                                        ).addOnSuccessListener {
                                            //Log.i(TAG.INFO, "chat member update successfully")
                                            send("${fromUser}_${toUser}", message)
                                        }.addOnFailureListener {
                                            //Log.e(TAG.ERROR, "chat member update failed ${it.message}")
                                        }
                                }
                            }
                    }
                }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun send(
        doc: String,
        message: Message
    ) {
        try {
            chatUser.documentId = doc
            dbRepo.insertUser(chatUser)
            val chatUserId = message.chatUserId
            message.chatUserId = null  // chatUserId field is being used only for relation query, changing to null will ignore this field
            message.status = 1
            message.chatUsers = arrayListOf(message.from, message.to)
            msgCollection.document(doc).collection(FireStoreCollection.MESSAGE)
                .document(message.createdAt.toString()).set(
                    message,
                    SetOptions.merge()
                ).addOnSuccessListener {
                    //Log.i(TAG.INFO, "Message sender Sucesss ${message.createdAt}")
                    message.chatUserId = chatUserId
                    listener.onSuccess(message)
                }.addOnFailureListener {
                    message.chatUserId = chatUserId
                    message.status = 4
                    //Log.e(TAG.ERROR, "Message sender Failed ${it.message}")
                    listener.onFailed(message)
                }
            /*          msgCollection.document(doc)
                          .update("messages",
                              FieldValue.arrayUnion(message.serializeToMap())).addOnSuccessListener {
                              LogMessage.v("Message sender Sucesss ${message.textMessage?.text}")
                              listener.onSuccess(message)
                          }.addOnFailureListener {
                              message.status=4
                              LogMessage.v("Message sender Failed ${it.message}")
                              listener.onFailed(message)
                          }*/
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}