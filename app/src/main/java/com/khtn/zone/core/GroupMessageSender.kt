package com.khtn.zone.core

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.khtn.zone.database.data.Group
import com.khtn.zone.database.data.GroupMessage
import com.khtn.zone.utils.FireStoreCollection

interface OnGrpMessageResponse {
    fun onSuccess(message: GroupMessage)
    fun onFailed(message: GroupMessage)
}

class GroupMessageSender(private val groupCollection: CollectionReference) {
    fun sendMessage(
        message: GroupMessage,
        group: Group,
        listener: OnGrpMessageResponse
    ){
        message.status[0] = 1
        groupCollection.document(group.id).collection(FireStoreCollection.GROUP_MESSAGE)
            .document(message.createdAt.toString()).set(message, SetOptions.merge())
            .addOnSuccessListener {
                listener.onSuccess(message)
            }.addOnFailureListener {
                message.status[0] = 4
                listener.onFailed(message)
            }
    }
}