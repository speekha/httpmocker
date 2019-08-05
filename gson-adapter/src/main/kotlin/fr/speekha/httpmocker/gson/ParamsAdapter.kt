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

internal class ParamsAdapter : TypeAdapter<ParamsAdapter.ParamList>() {

    class ParamList(map: Map<String, String?> = emptyMap()) : ArrayList<Pair<String, String?>>(map.entries.map { it.key to it.value })

    override fun write(writer: JsonWriter?, params: ParamList?) {
        writer?.run {
            beginObject()
            serializeNulls = true
            params?.forEach {
                name(it.first)
                value(it.second)
            }
            serializeNulls = false
            endObject()
        }
    }

    override fun read(reader: JsonReader?): ParamList = reader?.run {
        val list = ParamList()
        beginObject()
        while (hasNext()) {
            val name = nextName()
            val value = if (peek() == JsonToken.NULL) {
                nextNull()
                null
            } else {
                nextString()
            }
            list += name to value
        }
        endObject()
        list
    } ?: ParamList()
}