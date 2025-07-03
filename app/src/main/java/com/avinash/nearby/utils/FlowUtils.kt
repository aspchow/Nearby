package com.avinash.nearby.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Created by Avinash Munnangi on 02/07/25.
 */

/**
 * */
fun <T> Flow<Set<T>>.emitOnChuckedOrDebounce(
    size: Int = 10,
    duration : Long = 1000L
): Flow<Set<T>> = channelFlow {

    var result: MutableSet<T>? = null
    var debounceJob: Job? = null

    val emitListAndClearDebounce: suspend (Set<T>) -> Unit = { listToEmit ->
        send(listToEmit)
        debounceJob?.cancel()
        debounceJob = null
        result = null
    }

    collect { items ->
        val accumulated = result ?: mutableSetOf<T>().also { result = it }
        accumulated.addAll(items)
        if (accumulated.size == size) {
            emitListAndClearDebounce.invoke(accumulated)
        }
        if (debounceJob != null) return@collect
        debounceJob = launch {
            delay(duration)
            emitListAndClearDebounce.invoke(accumulated)
        }
    }
}

