/*
 * Copyright 2019 David Blanc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.speekha.httpmocker.jackson.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import fr.speekha.httpmocker.jackson.model.KeyValue
import java.io.IOException

/**
 * Special deserializer allowing to have the same header several times (e.g. Set-Cookie)
 */
internal class KeyValueSerializer : JsonSerializer<List<KeyValue>>() {

    @Throws(IOException::class)
    override fun serialize(value: List<KeyValue>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        value.forEach {
            gen.writeStringField(it.key, it.value)
        }
        gen.writeEndObject()
    }
}
