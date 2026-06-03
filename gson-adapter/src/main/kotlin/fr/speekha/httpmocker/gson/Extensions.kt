/*
 * Copyright 2019-2021 David Blanc
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

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import fr.speekha.httpmocker.model.NamedParameter

internal fun JsonReader.readStringOrNull(): String? = if (peek() == JsonToken.NULL) {
    nextNull()
    null
} else {
    nextString()
}

internal fun <T> JsonReader.readList(list: MutableList<T>, initObject: (String, String?) -> T) {
    beginObject()
    while (hasNext()) {
        list += initObject(nextName(), readStringOrNull())
    }
    endObject()
}

internal fun <T> JsonWriter.writeList(
    params: List<T>?,
    transform: (T) -> NamedParameter
) {
    beginObject()
    serializeNulls = true
    params?.map(transform)?.forEach { (name, value) ->
        name(name)
        value(value)
    }
    serializeNulls = false
    endObject()
}
