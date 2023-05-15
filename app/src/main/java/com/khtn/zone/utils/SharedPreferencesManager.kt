package com.khtn.zone.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.khtn.zone.model.ModelMobile
import com.khtn.zone.model.UserProfile

class SharedPreferencesManager(context: Context) {
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

    fun saveProfile(profile: UserProfile){
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