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

    val country = MutableLiveData<Country>()

    val openMainAct = MutableLiveData<Boolean>()

    private val _state = MutableLiveData<ScreenState>(ScreenState.IdleState)

    val lastQuery = MutableLiveData<String>()

    val listOfQuery = arrayListOf("")

    private var timer: TimerTask? = null

    init {
        "Init SharedViewModel".printMeD()
    }

    fun getLastQuery(): LiveData<String> {
        return lastQuery
    }

    fun setLastQuery(query: String) {
        Timber.v("Last Query $query")
        listOfQuery.add(query)
        lastQuery.value = query
    }

    fun setState(state: ScreenState) {
        Timber.v("State $state")
        _state.value = state
    }

    fun getState(): LiveData<ScreenState> {
        return _state
    }

    fun setCountry(country: Country) {
        this.country.value = country
    }


    fun onFromSplash() {
        if (timer == null) {
            timer = Timer().schedule(2000) {
                openMainAct.postValue(true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        "onCleared SharedViewModel".printMeD()
    }

}