package io.github.asnaeb.kuery.query

import kotlinx.coroutines.flow.MutableStateFlow

class QueryState<Data>(initialData: Data? = null, initialUpdatedAt: Long = 0L) {
    val updatedAt = MutableStateFlow(initialUpdatedAt)
    val data = MutableStateFlow(initialData)
    val error = MutableStateFlow<Throwable?>(null)
    val isFirstFetched = MutableStateFlow(false)
    val isRefetching = MutableStateFlow(false)
    val isFirstFetching = MutableStateFlow(false)
    val isError = MutableStateFlow(false)
    val isSuccess = MutableStateFlow(false)
    val invalidated = MutableStateFlow(false)
}