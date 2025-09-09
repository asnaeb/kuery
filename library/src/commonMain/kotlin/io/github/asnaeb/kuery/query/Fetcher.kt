package io.github.asnaeb.kuery.query

fun interface Fetcher<Data, Params> {
    suspend fun fetch(params: Params): Data
}