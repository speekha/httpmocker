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

package fr.speekha.httpmocker.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import fr.speekha.httpmocker.gson.Header as JsonHeader

internal class HeaderAdapter : TypeAdapter<HeaderAdapter.HeaderList>() {

    class HeaderList(list: List<JsonHeader> = emptyList()) : ArrayList<JsonHeader>(list)

    override fun write(writer: JsonWriter?, headers: HeaderList?) {
        writer?.run {
            beginObject()
            serializeNulls = true
            headers?.forEach {
                name(it.name)
                value(it.value)
            }
            serializeNulls = false
            endObject()
        }
    }

    override fun read(reader: JsonReader?): HeaderList = reader?.run {
        val list = HeaderList()
        beginObject()
        while (hasNext()) {
            val name = nextName()
            val value = if (peek() == JsonToken.NULL) {
                nextNull()
                null
            } else {
                nextString()
            }
            list += JsonHeader(name, value)
        }
        endObject()
        list
    } ?: HeaderList()
}