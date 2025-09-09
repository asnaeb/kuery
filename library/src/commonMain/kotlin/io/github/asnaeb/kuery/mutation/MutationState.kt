package io.github.asnaeb.kuery.mutation

import kotlinx.coroutines.flow.MutableStateFlow

class MutationState<T> {
    val data = MutableStateFlow<T?>(null)
    val error = MutableStateFlow<Throwable?>(null)
    val isMutating = MutableStateFlow(false)
    val isError = MutableStateFlow(false)
    val isSuccess = MutableStateFlow(false)
}