package io.github.asnaeb.kuery.query

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.github.asnaeb.kuery.ProvidedQueryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class Query<Data, Params>(
    val key: QueryKey<Params>,
    @PublishedApi
    internal val state: QueryState<Data>,
    private val fetcher: Fetcher<Data, Params>,
    private val setExternalData: (Data?) -> Unit,
    private val setExternalUpdatedAt: (Long) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    var data
        get() = state.data.value
        set(value) {
            state.data.value = value
            setExternalData(value)
        }

    private fun runJob(block: suspend CoroutineScope.() -> Unit) {
        cancel()
        job = scope.launch(block = block)
    }

    private suspend fun fetchRetry(retries: Int = 0): Data {
        return try {
            fetcher.fetch(key.params)
        }
        catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }

            if (retries > 0) {
                fetchRetry(retries - 1)
            }

            throw e
        }
    }

    private suspend fun fetchSuspending(retries: Int = 0) {
        val loading = if (state.isFirstFetched.value) state.isRefetching else state.isFirstFetching

        loading.value = true

        try {
            data = fetchRetry(retries)

            if (!state.isFirstFetched.value) {
                state.isFirstFetched.value = true
            }

            if (state.error.value != null) {
                state.error.value = null
            }

            if (state.isError.value) {
                state.isError.value = false
            }

            if (!state.isSuccess.value) {
                state.isSuccess.value = true
            }

            if (state.invalidated.value) {
                state.invalidated.value = false
            }

            @OptIn(ExperimentalTime::class)
            val now = Clock.System.now().epochSeconds

            state.updatedAt.value = now
            setExternalUpdatedAt(now)
        }
        catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }

            println(e)
            e.printStackTrace()
            data = null
            state.error.value = e
            state.isError.value = true
            state.isSuccess.value = false
        }
        finally {
            loading.value = false
        }
    }

    fun fetch() = runJob {
        fetchSuspending()
    }

    fun cancel() {
        job?.cancel()
    }

    fun invalidate() {
        state.invalidated.value = true
    }

    suspend fun ensureData(): Data {
        if (!state.isFirstFetched.value) {
            fetch()
            job?.join()
        }

        if (state.isError.value && state.error.value != null) {
            throw state.error.value!!
        }

        @Suppress("UNCHECKED_CAST")
        return data as Data
    }

    @Composable
    fun subscribe(
        staleTime: Duration? = null,
        enabled: Boolean? = null,
        retries: Int? = null
    ): SubscribeQuery<Data, Params> {
        val queryManager = ProvidedQueryManager()
        val updatedAt by state.updatedAt.collectAsState()
        val isFirstFetched by state.isFirstFetched.collectAsState()
        val invalidated by state.invalidated.collectAsState()

        val staleTime = staleTime ?: queryManager.defaultStaleTime
        val retries = retries ?: queryManager.defaultRetries
        val enabled = enabled ?: queryManager.defaultEnabled

        DisposableEffect(updatedAt, isFirstFetched, invalidated) {
            @OptIn(ExperimentalTime::class)
            val isExpired = (updatedAt + staleTime.inWholeMilliseconds) <= Clock.System.now().epochSeconds
            val isStale = !isFirstFetched || isExpired || invalidated

            if (enabled && isStale) {
                if (retries > 0) {
                    runJob {
                        fetchSuspending(retries)
                    }
                }
                else {
                    fetch()
                }
            }

            onDispose {
                cancel()
            }
        }

        return remember { SubscribeQuery(this) }
    }
}