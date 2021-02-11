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

package fr.speekha.httpmocker.moshi

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import fr.speekha.httpmocker.model.NamedParameter

internal fun JsonWriter.writeList(list: Iterable<NamedParameter>) {
    beginObject()
    serializeNulls = true
    list.forEach {
        name(it.name)
        value(it.value)
    }
    serializeNulls = false
    endObject()
}

internal fun <T> JsonReader.readList(list: T, addObject: T.(String, String?) -> Unit): T {
    beginObject()
    while (hasNext()) {
        list.addObject(nextName(), readStringOrNull())
    }
    endObject()
    return list
}

internal fun JsonReader.readStringOrNull(): String? = if (peek() != JsonReader.Token.NULL) {
    nextString()
} else {
    nextNull<Unit>()
    null
}
