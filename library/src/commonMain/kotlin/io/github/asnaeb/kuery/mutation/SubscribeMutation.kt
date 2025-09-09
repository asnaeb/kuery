package io.github.asnaeb.kuery.mutation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class SubscribeMutation<Data, Params>(
    private val mutator: Mutator<Data, Params>,
    private val isParentMutating: MutableStateFlow<Boolean>,
    init: (MutationBuilder<Data, Params>.() -> Unit)? = null
) {
    private var job: Job? = null
    private val builder = init?.let(MutationBuilder<Data, Params>()::apply)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val state: MutationState<Data> = MutationState()

    val data
        @Composable
        get() = state.data.collectAsState().value

    val error
        @Composable
        get() = state.error.collectAsState().value

    val isMutating
        @Composable
        get() = state.isMutating.collectAsState().value

    val isSuccess
        @Composable
        get() = state.isSuccess.collectAsState().value

    val isError
        @Composable
        get() = state.isError.collectAsState().value

    fun mutate(args: Params) {
        if (job?.isActive == true) {
            job?.cancel()
        }

        job = scope.launch {
            state.isMutating.value = true
            isParentMutating.value = true

            builder?.onMutate?.apply {
                launch {
                    invoke(args)
                }
            }

            try {
                val data = mutator.mutate(args)

                state.data.value = data
                state.isSuccess.value = true
                state.isError.value = false

                builder?.onSuccess?.apply {
                    launch {
                        invoke(data)
                    }
                }
            }
            catch (e: Throwable) {
                if (e is CancellationException) {
                    builder?.onCancel?.apply {
                        launch {
                            invoke(e)
                        }
                    }

                    throw e
                }

                builder?.onError?.apply {
                    launch {
                        invoke(e)
                    }
                }

                state.isError.value = true
                state.isSuccess.value = false
                state.error.value = e

            }
            finally {
                builder?.onSettled?.apply {
                    launch {
                        invoke()
                    }
                }

                state.isMutating.value = false
                isParentMutating.value = false
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    operator fun component1() = ::mutate

    @Composable
    operator fun component2() = data

    @Composable
    operator fun component3() = error
}