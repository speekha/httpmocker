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
import com.google.gson.stream.JsonWriter
import fr.speekha.httpmocker.gson.model.Header as JsonHeader

internal class HeaderAdapter : TypeAdapter<HeaderAdapter.HeaderList>() {

    internal class HeaderList(list: List<JsonHeader> = emptyList()) : ArrayList<JsonHeader>(list)

    override fun write(writer: JsonWriter?, headers: HeaderList?) {
        writer?.writeList(headers) { it.name to it.value }
    }

    override fun read(reader: JsonReader?): HeaderList = HeaderList().also {
        reader?.run {
            readList(it) { name, value -> JsonHeader(name, value) }
        }
    }
}
