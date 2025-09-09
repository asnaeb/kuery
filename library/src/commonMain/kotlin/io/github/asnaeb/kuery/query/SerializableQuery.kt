package io.github.asnaeb.kuery.query

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
@KeepGeneratedSerializer
@Serializable(with = QueryManagerEntrySerializer::class)
data class SerializableQuery<Data>(
    var updatedAt: Long,
    var data: Data?
)

private class QueryManagerEntrySerializer<T>(dataSerializer: KSerializer<T>) : KSerializer<SerializableQuery<T>> {
    private val serializer = SerializableQuery.generatedSerializer(dataSerializer)

    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): SerializableQuery<T> = serializer.deserialize(decoder)

    override fun serialize(encoder: Encoder, value: SerializableQuery<T>) = serializer.serialize(encoder, value)
}