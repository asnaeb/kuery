package io.github.asnaeb.kuery

interface QueryPersister {
    suspend fun getItem(hashKey: String): String?
    suspend fun setItem(hashKey: String, value: String)
    suspend fun removeItem(hashKey: String)
    suspend fun getAll(): Map<String, String>
    suspend fun removeAll()
}