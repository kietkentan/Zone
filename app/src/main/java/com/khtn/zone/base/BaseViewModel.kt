package com.khtn.zone.base

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel: ViewModel() {
    private var error = MutableLiveData<Pair<String, String>>()
    private val errorMessage = MutableLiveData<String?>()
    private var netWorkError = MutableLiveData<Boolean>()

    fun getNetWorkError(): MutableLiveData<Boolean> {
        return this.netWorkError
    }

    fun setNetWorkError(mutableLiveData: MutableLiveData<Boolean>) {
        netWorkError = mutableLiveData
    }

    fun getError(): MutableLiveData<Pair<String, String>> {
        return error
    }

    fun setError(mutableLiveData: MutableLiveData<Pair<String, String>>) {
        error = mutableLiveData
    }

    fun getErrorMessage(): MutableLiveData<String?> {
        return errorMessage
    }

    fun setErrorMessage(error: String?) {
        errorMessage.postValue(error)
    }

    fun clearError() {
        errorMessage.value = null
    }

    @SuppressLint("LogNotTimber")
    open fun onError(error: String) {
        //Log.e(TAG.ERROR, error)
        errorMessage.postValue(error)
    }

    fun clearUIState() {
        netWorkError.value = false
        errorMessage.value = null
    }
}