package com.khtn.zone.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.khtn.zone.TYPE_NEW_GROUP
import com.khtn.zone.core.QueryCompleteListener
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.database.data.Group
import com.khtn.zone.di.GroupCollection
import com.khtn.zone.di.UserCollection
import com.khtn.zone.model.UserProfile
import com.khtn.zone.repo.DatabaseRepo
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.UiState
import com.khtn.zone.utils.UserUtils
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.toast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CreateGroupChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preference: SharedPreferencesManager,
    @UserCollection private val userCollection: CollectionReference,
    private val dbRepo: DatabaseRepo,
    @GroupCollection private val groupCollection: CollectionReference
): ViewModel() {
    private val _progressImg = MutableLiveData<Boolean>()
    val progressImg: LiveData<Boolean>
        get() = _progressImg

    private val _progressCreate = MutableLiveData(false)
    val progressCreate: LiveData<Boolean>
        get() = _progressCreate

    private val _imageUrl = MutableLiveData<String>()
    val imageUrl: LiveData<String>
        get() = _imageUrl

    private val _imageUri = MutableLiveData<Uri>()
    val imageUri: LiveData<Uri>
        get() = _imageUri

    val groupName = MutableLiveData("")

    private val _stateGroupCreate = MutableLiveData<UiState<*>>()
    val stateGroupCreate: LiveData<UiState<*>>
        get() = _stateGroupCreate

    private val _memberList = MutableLiveData<MutableList<ChatUser>>(mutableListOf())
    val memberList: LiveData<MutableList<ChatUser>>
        get() = _memberList

    private val _allContacts = MutableLiveData<List<ChatUser>>()
    val allContacts: LiveData<List<ChatUser>>
        get() = _allContacts

    private val _itemChange = MutableLiveData<Pair<ChatUser, Boolean>>()
    val itemChange: LiveData<Pair<ChatUser, Boolean>>
        get() = _itemChange

    private val _filterContacts = MutableLiveData<List<Pair<ChatUser, Boolean>>>()
    val filterContacts: LiveData<List<Pair<ChatUser, Boolean>>>
        get() = _filterContacts

    private val _queryState = MutableLiveData<UiState<*>>()
    val queryState: LiveData<UiState<*>>
        get() = _queryState

    private val _isFirstCall = MutableLiveData(true)
    val isFirstCall: LiveData<Boolean>
        get() = _isFirstCall

    private lateinit var chatUsers: List<ChatUser>
    private val storageRef = UserUtils.getStorageRef(context)
    private lateinit var onRemoveListener: ItemRemoveListener

    init {
        viewModelScope.launch(Dispatchers.IO) {
            chatUsers = dbRepo.getChatUserList().filter { it.locallySaved }
            if (chatUsers.isEmpty())
                startQuery()
        }
    }

    private fun startQuery() {
        try {
            _queryState.postValue(UiState.Loading)
            val success = UserUtils.updateContactsProfiles(onQueryCompleted)
            if (!success) {
                Timber.v("Recursion error")
                _queryState.postValue(UiState.Failure(0))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uploadProfileImage() {
        try {
            _progressImg.value = true
            _progressCreate.value = true
            val child = storageRef.child("group_${System.currentTimeMillis()}.jpg")
            val task = child.putFile(_imageUri.value!!)
            task.addOnSuccessListener {
                child.downloadUrl.addOnCompleteListener { taskResult ->
                    _progressImg.value = false
                    _imageUrl.value = taskResult.result.toString()
                }.addOnFailureListener { e ->
                    _progressImg.value = false
                    context.toast(e.message.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createGroup() {
        val memberList: ArrayList<ChatUser> = _memberList.value!! as ArrayList<ChatUser>
        _stateGroupCreate.value = UiState.Loading
        val gName = groupName.value + "_${Random.nextInt(0, 100)}"  //
        val profile: UserProfile = preference.getUserProfile()!!
        memberList.add(0, ChatUser(preference.getUid()!!, profile.userName, profile))
        val listOfProfiles = memberList.map { it.user } as ArrayList<UserProfile>
        val groupData = Group(
            id = gName,
            createdBy = preference.getUid()!!,
            createdAt = System.currentTimeMillis(),
            about = "",
            image = _imageUrl.value ?: "",
            members = null,
            profiles = listOfProfiles
        )

        groupCollection.document(gName).set(groupData, SetOptions.merge()).addOnSuccessListener {
            updateGroupInEveryUserProfile(groupData, memberList)
        }.addOnFailureListener { exception ->
            _stateGroupCreate.value = UiState.Failure(exception.hashCode())
            context.toast(exception.message.toString())
        }
    }

    fun setFilterContact(list: List<Pair<ChatUser, Boolean>>) {
        _filterContacts.value = list
    }

    fun checkedChange(check: Boolean, user: ChatUser) {
        val list = _memberList.value!!
        if (check && !list.contains(user))
            list.add(user)
        else if (!check && list.contains(user))
            list.remove(user)

        _itemChange.value = Pair(user, check)
        _memberList.value = list
    }

    fun setListener(listener: ItemRemoveListener) {
        onRemoveListener = listener
    }

    fun listMemberRemove(user: ChatUser) {
        val list = _memberList.value!!
        if (list.contains(user)) {
            list.remove(user)
            onRemoveListener.itemRemove(user)
        }

        _memberList.value = list
    }

    private fun updateGroupInEveryUserProfile(
        group: Group,
        memberList: ArrayList<ChatUser>
    ) {
        group.members = memberList
        group.profiles = ArrayList()
        val listOfIds = memberList.map { it.id }
        val batch = FirebaseFirestore.getInstance().batch()
        for (id in listOfIds) {
            val userDoc = userCollection.document(id)
            batch.set(
                userDoc,
                mapOf("groups" to FieldValue.arrayUnion(group.id)),
                SetOptions.merge()
            )
        }
        batch.commit().addOnSuccessListener {
            "Batch update success for group Creation".printMeD()
            _stateGroupCreate.value = UiState.Success(group)
            dbRepo.insertGroup(group)
            val groupData = Group(group.id)
            for (user in group.members!!) {
                val token = user.user.token
                if (token.isNotEmpty())
                    UserUtils.sendPush(
                        context = context,
                        type = TYPE_NEW_GROUP,
                        body = Json.encodeToString(groupData),
                        token = token,
                        to = user.id
                    )
            }
        }.addOnFailureListener { exception ->
            "Batch update failure ${exception.message} for group Creation".printMeD()
            _stateGroupCreate.value = UiState.Failure(exception.hashCode())
            context.toast(exception.message.toString())
        }
    }

    fun setIsFirstCall(b: Boolean) {
        _isFirstCall.value = b
    }

    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }

    fun setProgressCreate(b: Boolean) {
        _progressCreate.value = b
    }

    fun getChatList() = dbRepo.getAllChatUser()

    fun getMemberCount(): Int = _memberList.value?.size ?: 0

    fun getNameGroup(): String = groupName.value ?: ""

    fun setContactList(list: List<ChatUser>) {
        _allContacts.value = list
    }

    fun getContactList() = allContacts.value

    @Suppress("NAME_SHADOWING")
    private val onQueryCompleted = object : QueryCompleteListener {
        override fun onQueryCompleted(queriedList: ArrayList<UserProfile>) {
            try {
                val localContacts = UserUtils.fetchContacts(context)
                val finalList = ArrayList<ChatUser>()
                val queriedList = UserUtils.queriedList

                //set localSaved name to queried users
                for (doc in queriedList) {
                    val savedNumber = localContacts.firstOrNull { it ->
                        it.mobile.number == doc.mobile?.number
                    }
                    if (savedNumber != null) {
                        val chatUser = UserUtils.getChatUser(doc, chatUsers, savedNumber.name)
                        finalList.add(chatUser)
                    }
                }
                _queryState.value = UiState.Success(finalList)
                CoroutineScope(Dispatchers.IO).launch {
                    dbRepo.insertMultipleUser(finalList)
                }
                setDefaultValues()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun setDefaultValues() {
        // set default values
        UserUtils.totalRecursionCount = 0
        UserUtils.resultCount = 0
        UserUtils.queriedList.clear()
    }
}

interface ItemRemoveListener {
    fun itemRemove(user: ChatUser)
}