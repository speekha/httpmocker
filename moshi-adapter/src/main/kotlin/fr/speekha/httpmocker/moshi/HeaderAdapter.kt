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

package fr.speekha.httpmocker.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import fr.speekha.httpmocker.moshi.Header as JsonHeader

internal class HeaderAdapter {

    @FromJson
    fun headerFromJson(reader: JsonReader): List<JsonHeader> =
        reader.readList(mutableListOf()) { name, value ->
            add(
                fr.speekha.httpmocker.moshi.Header(name, value)
            )
        }

    @ToJson
    fun headerToJson(writer: JsonWriter, headers: List<JsonHeader>) =
        writer.writeList(headers.map { it.name to it.value })
}
