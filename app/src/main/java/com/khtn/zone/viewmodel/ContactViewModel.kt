package com.khtn.zone.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Filter
import com.khtn.zone.base.BaseViewModel
import com.khtn.zone.di.UserCollection
import com.khtn.zone.model.Contact
import com.khtn.zone.model.ModelMobile
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.UiState
import com.khtn.zone.utils.UserUtils
import com.khtn.zone.utils.Validator
import com.khtn.zone.utils.printMeD
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preference: SharedPreferencesManager,
    @UserCollection
    private val usersCollection: CollectionReference
): BaseViewModel() {
    private val _listContact: MutableLiveData<List<Contact>> = MutableLiveData()
    val listContact: LiveData<List<Contact>>
        get() = _listContact

    private val _listUser: MutableLiveData<UiState<List<UserProfile>>> = MutableLiveData()
    val listUser: LiveData<UiState<List<UserProfile>>>
        get() = _listUser

    @SuppressLint("Range")
    fun getContacts() {
        _listContact.value = UserUtils.fetchContacts(context)
    }

    fun checkContact() {
        val list: MutableList<String> = mutableListOf()
        for (item in _listContact.value!!) {
            var str = item.mobile.number.replace(" ", "")
            str = if (str.length > 9) str.substring(str.length - 9, str.length) else str
            list.add(str)
        }
        usersCollection
            .whereIn("mobile.number", list)
            .get()
            .addOnSuccessListener {
                val users = mutableListOf<UserProfile>()
                val uid = preference.getUid()
                for (document in it) {
                    val user = document.toObject(UserProfile::class.java)
                    if (user.uId != uid)
                        users.add(user)
                }
                _listUser.value = UiState.Success(users)
            }
            .addOnFailureListener {
                _listUser.value = UiState.Failure(it.hashCode())
            }
    }
}