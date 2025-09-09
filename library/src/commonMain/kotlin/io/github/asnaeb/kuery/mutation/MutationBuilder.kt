package io.github.asnaeb.kuery.mutation

import kotlin.coroutines.cancellation.CancellationException

@MutationDsl
class MutationBuilder<Data, Params> {
    @PublishedApi
    internal var onMutate: (suspend (Params) -> Unit)? = null

    @PublishedApi
    internal var onSuccess: (suspend (Data) -> Unit)? = null

    @PublishedApi
    internal var onError: (suspend (Throwable) -> Unit)? = null

    @PublishedApi
    internal var onCancel: (suspend (CancellationException) -> Unit)? = null

    @PublishedApi
    internal var onSettled: (suspend () -> Unit)? = null

    fun onMutate(fn: suspend (Params) -> Unit) {
        onMutate = fn
    }

    fun onSuccess(fn: suspend (Data) -> Unit) {
        onSuccess = fn
    }

    fun onError(fn: suspend (Throwable) -> Unit) {
        onError = fn
    }

    fun onCancel(fn: suspend (CancellationException) -> Unit) {
        onCancel = fn
    }

    fun onSettled(fn: suspend () -> Unit) {
        onSettled = fn
    }
}