package com.doc.paymentchecker.data.model

sealed interface Result<out R> {

    data object Loading : Result<Nothing>
    data class Success<T>(val value: T) : Result<T>
    data class Error(val value: Throwable) : Result<Nothing>
}