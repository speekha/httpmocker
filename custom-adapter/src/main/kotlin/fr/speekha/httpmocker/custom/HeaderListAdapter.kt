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

import fr.speekha.httpmocker.model.Header

class HeaderListAdapter : ObjectAdapter<List<Header>> {

    override fun fromJson(reader: JsonStringReader): List<Header> {
        val list = mutableListOf<Header>()
        reader.beginObject()
        while (reader.hasNext()) {
            val field = reader.readFieldName()
            val value = reader.readString()
            reader.next()
            list += Header(field, value)
        }
        reader.endObject()
        return list
    }

}
