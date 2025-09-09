package io.github.asnaeb.kuery.mutation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow

open class Mutation<Data, Params>(private val mutator: Mutator<Data, Params>) {
    internal val isMutating = MutableStateFlow(false)

    suspend fun mutate(args: Params): Data = mutator.mutate(args)

    @Composable
    fun subscribe(init: (MutationBuilder<Data, Params>.() -> Unit)? = null): SubscribeMutation<Data, Params> {
        val mutation = remember { SubscribeMutation(mutator, isMutating, init) }

        DisposableEffect(Unit) {
            onDispose {
                mutation.cancel()
            }
        }

        return mutation
    }
}