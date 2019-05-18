package fr.speekha.httpmocker.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.io.IOException

/**
 * Special deserializer allowing to have the same header several times (e.g. Set-Cookie)
 */
class HeadersDeserializer : JsonDeserializer<List<Header>>() {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(
        jsonParser: JsonParser,
        deserializationContext: DeserializationContext
    ): List<Header> = mutableListOf<Header>().apply {
        var token: String?
        do {
            token = jsonParser.nextFieldName()?.also { field ->
                val value = jsonParser.nextTextValue()
                add(Header(field, value))
            }
        } while (token != null)
    }
}
