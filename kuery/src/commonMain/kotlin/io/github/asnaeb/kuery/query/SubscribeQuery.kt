package io.github.asnaeb.kuery.query

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

class SubscribeQuery<Data, Params>(private val query: Query<Data, Params>) {
    @Composable
    fun <N> data(selector: (Data?) -> N): N {
        val currentData = data
        val currentSelector by rememberUpdatedState(selector)

        return remember(currentData) { currentSelector(currentData) }
    }

    val data: Data?
        @Composable
        get() = query.state.data.collectAsState().value

    val error: Throwable?
        @Composable
        get() = query.state.error.collectAsState().value

    val isSuccess: Boolean
        @Composable
        get() = query.state.isSuccess.collectAsState().value

    val isError: Boolean
        @Composable
        get() = query.state.isError.collectAsState().value

    val isRefetching: Boolean
        @Composable
        get() = query.state.isRefetching.collectAsState().value

    val isFirstFetching: Boolean
        @Composable
        get() = query.state.isFirstFetching.collectAsState().value

    val isFetching: Boolean
        @Composable
        get() = isFirstFetching || isRefetching

    @Composable
    operator fun component1() = data

    @Composable
    operator fun component2() = error
}