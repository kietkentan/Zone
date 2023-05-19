package com.khtn.zone.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.khtn.zone.model.ModelMobile
import com.khtn.zone.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharedPreferencesManager @Inject constructor(@ApplicationContext private val context: Context) {
    private var pref: SharedPreferences
    private var editor: SharedPreferences.Editor

    init {
        val sharedPreferences = context.getSharedPreferences(SharedPrefConstants.LOCAL_SHARED_PREF, Context.MODE_PRIVATE)
        this.pref = sharedPreferences
        this.editor = sharedPreferences.edit()
    }

    fun saveStringByKey(key: String, value: String): Boolean {
        return try {
            editor.run {
                putString(key, value)
                apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun retrieveStringByKey(key: String) =
        pref.getString(key, null)

    fun saveLongByKey(key: String, value: Long): Boolean {
        return try {
            editor.run {
                putLong(key, value)
                apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun retrieveLongByKey(key: String) =
        pref.getLong(key, 0)

    fun saveBooleanByKey(key: String, value: Boolean): Boolean {
        return try {
            editor.run {
                putBoolean(key, value)
                apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun retrieveBooleanByKey(key: String) =
        pref.getBoolean(key, false)

    fun retrieveBooleanByKey(key: String, defaultValue: Boolean) =
        pref.getBoolean(key, defaultValue)

    fun clearData() {
        try {
            editor.run {
                clear()
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setLogin(){
        retrieveBooleanByKey(SharedPrefConstants.LOGIN, true)}

    fun setLastDevice(same: Boolean){
        retrieveBooleanByKey(SharedPrefConstants.LAST_LOGGED_DEVICE_SAME, same)}

    fun setLogInTime(){
        saveLongByKey(SharedPrefConstants.LOGIN_TIME,System.currentTimeMillis())
    }

    fun getLogInTime() =
        pref.getLong(SharedPrefConstants.LOGIN_TIME, 0)

    fun setCurrentUser(id: String){
        saveStringByKey(SharedPrefConstants.ONLINE_USER, id)
    }

    fun clearCurrentUser() {
        setCurrentUser("")
    }

    fun getOnlineUser(): String {
        return retrieveStringByKey(SharedPrefConstants.ONLINE_USER) ?: ""
    }

    fun setCurrentGroup(id: String){
        saveStringByKey(SharedPrefConstants.ONLINE_GROUP, id)
    }

    fun clearCurrentGroup() {
        setCurrentGroup("")
    }

    fun getOnlineGroup(): String {
        return retrieveStringByKey(SharedPrefConstants.ONLINE_GROUP) ?: ""
    }


    fun isSameDevice()=
        pref.getBoolean(SharedPrefConstants.LAST_LOGGED_DEVICE_SAME, true)

    fun isLoggedIn()= pref.getBoolean(SharedPrefConstants.LOGIN, false)

    fun isNotLoggedIn()= !isLoggedIn()

    fun setUid(uid: String) =  saveStringByKey(SharedPrefConstants.UID, uid)

    fun getUid() = retrieveStringByKey(SharedPrefConstants.UID)

    fun updatePushToken(token: String){
        saveStringByKey(SharedPrefConstants.TOKEN, token)
    }

    fun getPushToken() = retrieveStringByKey(SharedPrefConstants.TOKEN)

    fun saveUserProfile(profile: UserProfile){
        saveStringByKey(SharedPrefConstants.USER, Gson().toJson(profile))
    }

    fun getUserProfile(): UserProfile?  {
        val str = retrieveStringByKey(SharedPrefConstants.USER)
        if (str.isNullOrBlank())
            return null
        return Gson().fromJson(str, UserProfile::class.java)
    }

    fun saveMobile(mobile: ModelMobile){
        saveStringByKey(SharedPrefConstants.MOBILE, Gson().toJson(mobile))
    }

    fun getMobile(): ModelMobile?  {
        val str = retrieveStringByKey(SharedPrefConstants.MOBILE)
        if (str.isNullOrBlank())
            return null
        return Gson().fromJson(str, ModelMobile::class.java)
    }
}