package com.khtn.zone.core

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.khtn.zone.database.data.GroupMessage
import com.khtn.zone.di.GroupCollection
import com.khtn.zone.utils.FireStoreCollection
import com.khtn.zone.utils.Utils.myIndexOfStatus
import com.khtn.zone.utils.Utils.myMsgStatus
import com.khtn.zone.viewmodel.asMap
import com.khtn.zone.viewmodel.serializeToMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupMsgStatusUpdater @Inject constructor(
    @GroupCollection
    private val groupCollection: CollectionReference,
    private val firestore: FirebaseFirestore
) {
    @SuppressLint("LogNotTimber")
    fun updateToDelivery(
        myUserId: String,
        messageList: List<GroupMessage>,
        vararg groupId: String
    ) {
        try {
            val batch = firestore.batch()
            for (id in groupId) {
                val msgSubCollection = groupCollection.document(id).collection(FireStoreCollection.GROUP_MESSAGE)
                val filterList = messageList
                    .filter {
                        it.from != myUserId && myMsgStatus(
                            myUserId,
                            it
                        ) == 0 && it.groupId == id
                    }
                    .map {
                        val myIndex = myIndexOfStatus(myUserId, it)
                        it.status[myIndex] = 2
                        it.deliveryTime[myIndex] = System.currentTimeMillis()
                        it
                    }
                for (msg in filterList) {
                    //Log.i(TAG.INFO, "message date ${msg.deliveryTime}")
                    batch.update(
                        msgSubCollection
                            .document(msg.createdAt.toString()), msg.asMap()
                    )
                }
            }
            batch.commit().addOnSuccessListener {
                //Log.i(TAG.INFO, "Batch update success from group")
            }.addOnFailureListener {
                //Log.e(TAG.INFO, "Batch update failure ${it.message} from group")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("LogNotTimber")
    fun updateToSeen(
        myUserId: String,
        messageList: List<GroupMessage>,
        groupId: String
    ) {
        val batch = firestore.batch()
        val currentTime = System.currentTimeMillis()
        val msgSubCollection = groupCollection.document(groupId).collection(FireStoreCollection.GROUP_MESSAGE)
        val filterList = messageList
            .filter { it.from != myUserId && myMsgStatus(myUserId, it) < 3 }
            .map {
                val myIndex = myIndexOfStatus(myUserId, it)
                it.status[myIndex] = 3
                it.deliveryTime[myIndex] = if (it.deliveryTime[myIndex] == 0L)
                    currentTime else it.deliveryTime[myIndex]
                it.seenTime[myIndex] = currentTime
                it
            }

        if (filterList.isNotEmpty()) {
            for (msg in filterList) {
                batch.update(
                    msgSubCollection
                        .document(msg.createdAt.toString()), msg.serializeToMap()
                )
            }
        }
        batch.commit().addOnSuccessListener {
            //Log.i(TAG.INFO, "Seen Batch update success from group")
        }.addOnFailureListener {
            //Log.e(TAG.ERROR, "Seen Batch update failure ${it.message} from group")
        }
    }
}