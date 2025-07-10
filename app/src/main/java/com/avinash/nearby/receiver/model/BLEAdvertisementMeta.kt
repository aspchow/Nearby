package com.avinash.nearby.receiver.model

/**
 * Created by Avinash Munnangi on 09/07/25.
 */
sealed interface BLEAdvertisementMeta {
    data class Advertising(val advertisingName: String) : BLEAdvertisementMeta
    data class AdvertisementFailed(val advertisingName: String) : BLEAdvertisementMeta
    data object Stopped : BLEAdvertisementMeta
    data object NotStarted : BLEAdvertisementMeta
}