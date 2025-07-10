package com.avinash.nearby.receiver.service

import com.avinash.nearby.receiver.model.BLEAdvertisementMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Avinash Munnangi on 09/07/25.
 */
@Singleton
class AdvertisingServiceRepository @Inject constructor() {

    private val _serviceMeta =
        MutableStateFlow<BLEAdvertisementMeta>(BLEAdvertisementMeta.NotStarted)
    val serviceMeta = _serviceMeta.asStateFlow()

    fun updateServiceMeta(meta: BLEAdvertisementMeta) {
        _serviceMeta.value = meta
    }
}