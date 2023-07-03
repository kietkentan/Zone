package com.khtn.zone.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.khtn.zone.model.Country
import com.khtn.zone.utils.ScreenState
import com.khtn.zone.utils.printMeD
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

@HiltViewModel
class SharedViewModel @Inject constructor() : ViewModel() {

    private val _country = MutableLiveData<Country>()
    val country: LiveData<Country>
        get() = _country

    private val _openMainActivity = MutableLiveData<Boolean>()
    val openMainActivity: LiveData<Boolean>
        get() = _openMainActivity

    private val _state = MutableLiveData<ScreenState>(ScreenState.IdleState)
    val state: LiveData<ScreenState>
        get() = _state

    private val _lastQuery = MutableLiveData<String>()
    val lastQuery: LiveData<String>
        get() = _lastQuery

    val listOfQuery = arrayListOf("")
    private var timer: TimerTask? = null

    init {
        "Init SharedViewModel".printMeD()
    }

    fun setLastQuery(query: String) {
        Timber.v("Last Query $query")
        listOfQuery.add(query)
        _lastQuery.value = query
    }

    fun setState(state: ScreenState) {
        Timber.v("State $state")
        _state.value = state
    }

    fun setCountry(country: Country) {
        _country.value = country
    }


    fun onFromSplash() {
        if (timer == null) {
            timer = Timer().schedule(1000) {
                _openMainActivity.postValue(true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        "onCleared SharedViewModel".printMeD()
    }

}