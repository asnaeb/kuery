package io.github.asnaeb.kuery.mutation

fun interface Mutator<Data, Params> {
    suspend fun mutate(params: Params): Data
}