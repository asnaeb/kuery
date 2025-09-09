package io.github.asnaeb.kuery.query

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Serializable
data class QueryKey<Params>(val id: String, val params: Params) {
    constructor(id: Enum<*>, params: Params) : this(id.name, params)
}

fun QueryKey(id: Enum<*>) = QueryKey(id, Unit)
fun QueryKey(id: String) = QueryKey(id, Unit)

inline fun <reified Params> QueryKey<Params>.hash() = Json.encodeToString(
    QueryKey.serializer(serializer<Params>()),
    this
)