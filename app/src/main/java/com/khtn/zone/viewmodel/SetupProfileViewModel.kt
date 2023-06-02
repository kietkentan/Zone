package com.khtn.zone.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.firestore.SetOptions
import com.khtn.zone.base.BaseViewModel
import com.khtn.zone.model.ModelDeviceDetails
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SetupProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mPreferencesManager: SharedPreferencesManager
): BaseViewModel() {
    private val _progressProPic = MutableLiveData(false)
    val progressProPic: LiveData<Boolean>
        get() = _progressProPic

    private val _progressSetup = MutableLiveData(false)
    val progressSetup: LiveData<Boolean>
        get() = _progressSetup

    private val _profileUpdateState = MutableLiveData<UiState<Any>>()
    val profileUpdateState: LiveData<UiState<Any>>
        get() = _profileUpdateState

    private val _checkUserNameState = MutableLiveData<UiState<Any>>()
    val checkUserNameState: LiveData<UiState<Any>>
        get() = _checkUserNameState

    private val _name = MutableLiveData<String>()
    val name: LiveData<String>
        get() = _name

    private val _profilePicUrl = MutableLiveData<String>()
    val profilePicUrl: LiveData<String>
        get() = _profilePicUrl

    private val _errorSetup = MutableLiveData<String>()
    val errorSetup: LiveData<String>
        get() = _errorSetup

    private val storageRef = UserUtils.getStorageRef(context)
    private val docuRef = UserUtils.getDocumentRef(context)
    private var about = ""
    private var createdAt: Long = System.currentTimeMillis()

    init {
        //LogMessage.v("ProfileViewModel")
        val userProfile = mPreferencesManager.getUserProfile()
        userProfile?.let {
            _name.value = userProfile.userName
            _profilePicUrl.value = userProfile.image
            about = userProfile.about
            createdAt = userProfile.createdAt ?: System.currentTimeMillis()
        }
    }

    fun uploadProfileImage(imagePath: Uri) {
        try {
            _progressProPic.value = true
            val child = storageRef.child("profile_picture_${System.currentTimeMillis()}.jpg")
            val task = child.putFile(imagePath)
            task.addOnSuccessListener {
                child.downloadUrl.addOnCompleteListener { taskResult ->
                    _progressProPic.value = false
                    _profilePicUrl.value = taskResult.result.toString()
                }.addOnFailureListener {
                    OnFailureListener { e ->
                        _progressProPic.value = false
                        context.toast(e.message.toString())
                    }
                }
            }.addOnProgressListener { taskSnapshot ->
                100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun storeProfileData() {
        try {
            _profileUpdateState.value = UiState.Loading
            val profile = UserProfile(
                mPreferencesManager.getUid()!!,
                createdAt,
                System.currentTimeMillis(),
                _profilePicUrl.value!!,
                _name.value!!.lowercase(Locale.getDefault()),
                about,
                mobile = mPreferencesManager.getMobile(),
                token = mPreferencesManager.getPushToken().toString(),
                deviceDetails = Json.decodeFromString<ModelDeviceDetails>(
                    UserUtils.getDeviceInfo(context).toString()
                )

            )
            docuRef.set(profile, SetOptions.merge()).addOnSuccessListener {
                mPreferencesManager.saveUserProfile(profile)
                _profileUpdateState.value = UiState.Success(it)
            }.addOnFailureListener { e ->
                context.toast(e.message.toString())
                _profileUpdateState.value = UiState.Failure(e.hashCode())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setProgressSetup(progress: Boolean) {
        _progressSetup.value = progress
    }

    fun setName(name: String) {
        _name.value = name
    }

    fun setError(error: String) {
        _errorSetup.value = error
    }

    override fun onCleared() {
        "ProfileViewModel Cleared".printMeD()
        super.onCleared()
    }
}