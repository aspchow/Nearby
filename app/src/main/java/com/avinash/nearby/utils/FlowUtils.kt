package com.avinash.nearby.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Created by Avinash Munnangi on 02/07/25.
 */


fun <T, C : Collection<T>> Flow<C>.emitOnCountOtDebounce(
    debounceTime: Long = 1000L,
    count: Int = 10
): Flow<C> = channelFlow {
    var lastItem: C? = null
    var debounceJob: Job? = null
    collectLatest { items ->
        lastItem = items
        if (items.size % count == 0) {
            debounceJob?.cancel()
            send(items)
        }
        if (debounceJob?.isActive == true) return@collectLatest
        debounceJob = launch {
            delay(debounceTime)
            lastItem?.let { send(it) }
        }
    }
}



