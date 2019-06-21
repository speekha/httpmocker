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

package fr.speekha.httpmocker.custom

class MapAdapter : ObjectAdapter<Map<String, String>> {

    override fun fromJson(reader: JsonStringReader): Map<String, String> {
        val map = mutableMapOf<String, String>()
        reader.beginObject()
        while (reader.hasNext()) {
            val field = reader.readFieldName()
            val value = reader.readString()
            reader.next()
            map += field to value
        }
        reader.endObject()
        return map
    }

}
