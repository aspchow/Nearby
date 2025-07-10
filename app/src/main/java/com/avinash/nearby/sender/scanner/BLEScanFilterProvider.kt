package com.avinash.nearby.sender.scanner

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import com.avinash.nearby.utils.BLEConsts
import java.util.UUID
import javax.inject.Inject

/**
 * Created by Avinash Munnangi on 01/07/25.
 */
class BLEScanFilterProvider @Inject constructor() {
    fun provideScanFilter(): List<ScanFilter> {
        return listOf(
            ScanFilter.Builder().apply {
                if (BLEConsts.ENABLE_SERVICE_FILTER)
                    setServiceUuid(ParcelUuid(UUID.fromString(BLEConsts.SERVICE_UUID)))
            }
                .build()
        )
    }
}