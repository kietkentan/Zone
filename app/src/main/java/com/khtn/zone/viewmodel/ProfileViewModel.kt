package com.khtn.zone.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.UploadTask
import com.khtn.zone.R
import com.khtn.zone.model.ModelMobile
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.UiState
import com.khtn.zone.utils.UserUtils
import com.khtn.zone.utils.toast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preference: SharedPreferencesManager
): ViewModel() {
    private var userProfile = preference.getUserProfile()

    private val _userName = MutableLiveData<String>(userProfile?.userName)
    val userName: LiveData<String>
        get() = _userName

    private val _imageUrl = MutableLiveData<String>(userProfile?.image)
    val imageUrl: LiveData<String>
        get() = _imageUrl

    private val _profilePicPath = MutableLiveData<Uri>()
    val profilePicPath: LiveData<Uri>
        get() = _profilePicPath

    private val _about = MutableLiveData<String>(userProfile?.about)
    val about: LiveData<String>
        get() = _about

    private val _isUploading = MutableLiveData(false)
    val isUploading: LiveData<Boolean>
        get() = _isUploading

    private val _mobileData = MutableLiveData<ModelMobile>(userProfile?.mobile)
    val mobileData: LiveData<ModelMobile>
        get() = _mobileData

    private val _profileUpdateState = MutableLiveData<UiState<*>>()
    val profileUpdateState: LiveData<UiState<*>>
        get() = _profileUpdateState

    private val _isDarkMode = MutableLiveData(false)
    val isDarkMode: LiveData<Boolean>
        get() = _isDarkMode

    private val _isEnglish = MutableLiveData(false)
    val isEnglish: LiveData<Boolean>
        get() = _isEnglish

    private val storageRef = UserUtils.getStorageRef(context)
    private val documentRef = UserUtils.getDocumentRef(context)
    private lateinit var uploadTask: UploadTask

    fun getNumMobile(): String =
        "${mobileData.value?.country} ${mobileData.value?.number}"

    fun setIsDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    fun changeLanguage() {
        _isEnglish.value = !_isEnglish.value!!
    }

    fun setIsEnglish(b: Boolean) {
        _isEnglish.value = b
    }

    fun uploadProfileImage(imagePath: Uri) {
        try {
            _isUploading.value = true
            val child = storageRef.child("profile_picture_${System.currentTimeMillis()}.jpg")
            if (this::uploadTask.isInitialized && uploadTask.isInProgress)
                uploadTask.cancel()
            uploadTask = child.putFile(imagePath)
            uploadTask.addOnSuccessListener {
                child.downloadUrl.addOnCompleteListener { taskResult ->
                    _isUploading.value = false
                    _imageUrl.value = taskResult.result.toString()
                }.addOnFailureListener {
                    _isUploading.value = false
                    context.toast(it.message.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveChanges(name: String, strAbout: String, image: String) {
        name.lowercase(Locale.getDefault())
        updateProfileData(name, strAbout, image)
    }

    private fun updateProfileData(name: String, strAbout: String, image: String) {
        try {
            _profileUpdateState.value = UiState.Loading
            val profile = userProfile!!
            profile.userName = name
            profile.about = strAbout
            profile.image = image
            profile.updatedAt = System.currentTimeMillis()
            documentRef.set(profile, SetOptions.merge()).addOnSuccessListener {
                context.toast(context.getString(R.string.profile_updated))
                userProfile = profile
                preference.saveUserProfile(profile)
                _profileUpdateState.value = UiState.Success(null)
            }.addOnFailureListener { e ->
                context.toast(e.message.toString())
                _profileUpdateState.value = UiState.Failure(e.hashCode())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (this::uploadTask.isInitialized && uploadTask.isInProgress)
            uploadTask.cancel()
    }
}