package com.khtn.zone.core

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.database.data.Message
import com.khtn.zone.di.MessageCollection
import com.khtn.zone.repo.DatabaseRepo
import com.khtn.zone.utils.FireStoreCollection
import com.khtn.zone.viewmodel.serializeToMap
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageStatusUpdater @Inject constructor(
    @MessageCollection
    private val msgCollection: CollectionReference,
    private val firebaseFirestore: FirebaseFirestore,
    private val dbRepository: DatabaseRepo,
) {
    fun updateToDelivery(
        messageList: List<Message>,
        vararg chatUsers: ChatUser
    ) {
        val batch = firebaseFirestore.batch()
        for (chatUser in chatUsers) {
            if (chatUser.documentId.isNullOrBlank())
                continue
            val msgSubCollection =
                msgCollection.document(chatUser.documentId!!).collection(FireStoreCollection.MESSAGE)
            val filterList = messageList
                .filter { msg -> msg.status == 1 && msg.from == chatUser.id }
                .map {
                    it.chatUserId = null
                    it.status = 2
                    it.deliveryTime = System.currentTimeMillis()
                    it
                }
            if (filterList.isNotEmpty()) {
                for (msg in filterList) {
                    batch.update(
                        msgSubCollection
                            .document(msg.createdAt.toString()), msg.serializeToMap()
                    )
                }
                //dbRepository.insertMultipleMessage(filterList.toMutableList())
            }
        }
        batch.commit().addOnSuccessListener {
            Timber.v("Batch update success from home")
        }.addOnFailureListener {
            Timber.v("Batch update failure ${it.message} from home")
        }
    }

    fun updateToSeen(
        toUser: String,
        docId: String?,
        messageList: List<Message>
    ) {
        if (docId == null)
            return
        val msgSubCollection = msgCollection.document(docId).collection(FireStoreCollection.MESSAGE)
        val batch = firebaseFirestore.batch()
        val currentTime = System.currentTimeMillis()
        val filterList = messageList
            .filter { msg -> msg.from == toUser && msg.status != 3 }
            .map {
                it.status = 3
                it.chatUserId = it.chatUserId
                it.deliveryTime = it.deliveryTime
                it.seenTime = currentTime
                it
            }
        if (filterList.isNotEmpty()) {
            Timber.v("Size of list ${filterList.last().createdAt}")
            for (message in filterList) {
                batch.update(
                    msgSubCollection
                        .document(message.createdAt.toString()), message.serializeToMap()
                )
            }
            //dbRepository.insertMultipleMessage(filterList.toMutableList())

            batch.commit().addOnSuccessListener {
                Timber.v("All Message Seen Batch update success")
            }.addOnFailureListener {
                Timber.v("All Message Seen Batch update failure ${it.message}")
            }
        } else {
            Timber.v("All message already seen")
        }
    }
}