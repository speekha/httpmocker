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
import fr.speekha.httpmocker.model.NamedParameter

internal class ParamsAdapter : TypeAdapter<ParamsAdapter.ParamList>() {

    internal class ParamList(
        map: List<NamedParameter> = emptyList()
    ) : ArrayList<NamedParameter>(map)

    override fun write(writer: JsonWriter?, params: ParamList?) {
        writer?.writeList(params) { it }
    }

    override fun read(reader: JsonReader?): ParamList = ParamList().also {
        reader?.run {
            readList(it) { name, value -> NamedParameter(name, value) }
        }
    }
}
