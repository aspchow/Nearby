package com.avinash.nearby.sender.scanner

/**
 * Created by Avinash Munnangi on 01/07/25.
 */
sealed interface ScanState {
    data object IDLE : ScanState
    data object Scanning : ScanState
    data object Stopped : ScanState
    data class Error(val message: String) : ScanState
}