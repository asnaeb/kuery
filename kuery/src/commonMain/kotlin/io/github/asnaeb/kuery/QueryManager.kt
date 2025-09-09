package io.github.asnaeb.kuery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import io.github.asnaeb.kuery.mutation.Mutation
import io.github.asnaeb.kuery.mutation.Mutator
import io.github.asnaeb.kuery.query.Fetcher
import io.github.asnaeb.kuery.query.Query
import io.github.asnaeb.kuery.query.QueryKey
import io.github.asnaeb.kuery.query.QueryState
import io.github.asnaeb.kuery.query.SerializableQuery
import io.github.asnaeb.kuery.query.hash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class QueryManager(
    internal val defaultStaleTime: Duration = 5.minutes,
    internal val defaultEnabled: Boolean = true,
    internal val defaultRetries: Int = 0,
    @PublishedApi
    internal val persister: QueryPersister? = null,
    @PublishedApi
    internal val scope: CoroutineScope = CoroutineScope(Dispatchers.Default) + SupervisorJob()
) {
    init {
        if (persister != null) {
            scope.launch {
                persister.getAll().forEach {
                    (key, value) ->  launch {
                        serializableQueries[key] = Json.decodeFromString<SerializableQuery<*>>(value)
                    }
                }
            }
        }
    }

    @PublishedApi
    internal val serializableQueries = mutableMapOf<String, SerializableQuery<*>>()

    @PublishedApi
    internal val queries = mutableMapOf<QueryKey<*>, Query<*, *>>()

    private val mutations = mutableMapOf<String, Mutation<*, *>>()

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal inline fun <reified Data> getTypedSerializable(hashKey: String) = serializableQueries[hashKey]
        .takeIf { it?.data is Data? } as SerializableQuery<Data>?

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal inline fun <reified Data> getTypedQuery(key: QueryKey<*>) = queries[key]
        .takeIf { it != null && it.data is Data? && key == it.key } as? Query<Data, *>

    @PublishedApi
    internal inline fun <reified Data> persistSerializable(hashKey: String, entry: SerializableQuery<Data>) {
        persister?.apply {
            scope.launch {
                setItem(hashKey, Json.encodeToString(entry))
            }
        }
    }

    @PublishedApi
    internal inline fun <reified Data> updateSerializable(hashKey: String, data: Data?) {
        getTypedSerializable<Data>(hashKey)?.let {
            it.data = data
            persistSerializable(hashKey, it)
        }
    }

    @PublishedApi
    internal inline fun <reified Data> updateSerializable(hashKey: String, updatedAt: Long) {
        getTypedSerializable<Data>(hashKey)?.let {
            it.updatedAt = updatedAt
            persistSerializable(hashKey, it)
        }
    }

    inline fun <reified Data, reified Params> createQuery(
        key: QueryKey<Params>,
        fetcher: Fetcher<Data, Params>
    ): Query<Data, Params> {
        @Suppress("UNCHECKED_CAST")
        getTypedQuery<Data>(key)?.let {
            return it as Query<Data, Params>
        }

        val hashKey = key.hash()

        if (!serializableQueries.containsKey(hashKey)) {
            serializableQueries[hashKey] = SerializableQuery(updatedAt = 0L, data = null)
        }

        val entry = getTypedSerializable<Data>(hashKey)

        val query = Query(
            key = key,
            state = QueryState(entry?.data, entry?.updatedAt ?: 0L),
            setExternalData = { updateSerializable<Data>(hashKey, it) },
            setExternalUpdatedAt = { updateSerializable<Data>(hashKey, it) },
            fetcher = fetcher
        )

        queries[key] = query

        return query
    }

    inline fun <reified Data, reified Params> createQuery(
        id: String,
        fetcher: Fetcher<Data, Params>
    ): (Params) -> Query<Data, Params> = { params: Params -> createQuery(QueryKey(id, params), fetcher) }

    inline fun < reified Data, reified Params> createQuery(
        id: Enum<*>,
        fetcher: Fetcher<Data, Params>
    ): (Params) -> Query<Data, Params> = { params: Params -> createQuery(QueryKey(id, params), fetcher) }

    inline fun <reified Data> createQuery(
        id: String,
        fetcher: Fetcher<Data, Unit>
    ): Query<Data, Unit> = createQuery(QueryKey(id), fetcher)

    inline fun <reified Data> createQuery(
        id: Enum<*>,
        fetcher: Fetcher<Data, Unit>
    ): Query<Data, Unit> = createQuery(QueryKey(id), fetcher)

    fun <Data, Params> createMutation(id: String, mutator: Mutator<Data, Params>): Mutation<Data, Params> {
        if (id !in mutations) {
            mutations[id] = Mutation(mutator)
        }

        @Suppress("UNCHECKED_CAST")
        return mutations[id]!! as Mutation<Data, Params>
    }

    fun <Data, Params> createMutation(
        id: Enum<*>,
        mutator: Mutator<Data, Params>
    ): Mutation<Data, Params> = createMutation(id.name, mutator)

    @JvmName("createVoidMutation")
    fun <Data> createMutation(
        id: String,
        mutator: Mutator<Data, Unit>
    ): Mutation<Data, Unit> = createMutation<Data, Unit>(id, mutator)

    @JvmName("createVoidMutation")
    fun <Data> createMutation(
        id: Enum<*>,
        mutator: Mutator<Data, Unit>
    ): Mutation<Data, Unit> = createMutation<Data, Unit>(id.name, mutator)

    @Composable
    private fun isMutating(): Boolean {
        val states = remember(mutations.values) {
            mutations.values.map { it.isMutating }
        }

        val combined = remember {
            combine(states) { s -> s.any { it } }
        }

        return combined.collectAsState(states.any { it.value }).value
    }

    @Composable
    private fun isFetching(): Boolean {
        val states = remember(queries.values) {
            queries.values.map { it.state.isRefetching }
        }

        val combined = remember {
            combine(states) { s -> s.any { it } }
        }

        return combined.collectAsState(states.any { it.value }).value
    }
}