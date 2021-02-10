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
import fr.speekha.httpmocker.model.NamedParameter

internal class ParamAdapter {

    @FromJson
    fun paramFromJson(reader: JsonReader): Map<String, String?> =
        reader.readList(mutableMapOf()) { name, value -> put(name, value) }

    @ToJson
    fun paramToJson(writer: JsonWriter, params: Map<String, String?>) =
        writer.writeList(params.entries.map { NamedParameter(it.key, it.value) })
}
