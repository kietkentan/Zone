package com.khtn.zone.core

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.khtn.zone.utils.UserUtils
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.FireStoreCollection
import timber.log.Timber

interface QueryCompleteListener {
    fun onQueryCompleted(queriedList: ArrayList<UserProfile>)
}

class ContactsQuery(
    val list: ArrayList<String>,
    val position: Int,
    val listener: QueryCompleteListener
){
    private val usersCollection: CollectionReference = FirebaseFirestore.getInstance().collection(FireStoreCollection.USER)

    fun makeQuery() {
        try {
            usersCollection.whereIn(FireStoreCollection.MOBILE_NUMBER, list).get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val contact = document.toObject(UserProfile::class.java)
                        UserUtils.queriedList.add(contact)
                    }
                    UserUtils.resultCount += 1
                    if(UserUtils.resultCount == UserUtils.totalRecursionCount){
                        listener.onQueryCompleted(UserUtils.queriedList)
                    }
                }
                .addOnFailureListener { exception ->
                    Timber.wtf("Error getting documents: ${exception.message}")
                    UserUtils.resultCount += 1
                    if(UserUtils.resultCount == UserUtils.totalRecursionCount)
                        listener.onQueryCompleted(UserUtils.queriedList)
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}