package com.khtn.zone.utils

sealed class UiState<out T> {
    object Loading: UiState<Nothing>()
    data class Success<out T>(val data: T): UiState<T>()
    data class Failure(val errorCode: Int?): UiState<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Failure -> "Error[exception=$errorCode]"
            is Loading -> "Loading"
        }
    }
}