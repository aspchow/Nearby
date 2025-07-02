package com.avinash.nearby.sender.scanner

import android.bluetooth.le.ScanFilter
import javax.inject.Inject

/**
 * Created by Avinash Munnangi on 01/07/25.
 */
class BLEScanFilterProvider @Inject constructor() {

    fun provideScanFilter(): List<ScanFilter> {
        return emptyList()
    }
}