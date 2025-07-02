package com.avinash.nearby.sender.model

/**
 * Created by Avinash Munnangi on 01/07/25.
 */

sealed interface BLEError{
    data class PermissionDenied(val permission: String) : BLEError
}

@Suppress("UNCHECKED_CAST")
class BLEResult<T> private constructor(private val value: Any?){
    fun isSuccess() = value !is BLEError

    fun getSuccess() : T{
        return value as T
    }
    companion object {
        fun <T> success(value: T): BLEResult<T> = BLEResult(value)
        fun <T> failure(error: BLEError): BLEResult<T> = BLEResult(error)
    }
}