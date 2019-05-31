package fr.speekha.httpmocker.model

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.io.IOException

/**
 * Special deserializer allowing to have the same header several times (e.g. Set-Cookie)
 */
class HeadersSerializer : JsonSerializer<List<Header>>() {

    @Throws(IOException::class)
    override fun serialize(value: List<Header>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        value.forEach {
            gen.writeStringField(it.name, it.value)
        }
        gen.writeEndObject()
    }

}
