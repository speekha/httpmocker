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

package fr.speekha.httpmocker.custom.adapters

import fr.speekha.httpmocker.custom.JsonStringReader

internal abstract class BaseObjectAdapter<T : Any> :
    ObjectAdapter<T> {

    override fun fromJson(reader: JsonStringReader): T {
        reader.beginObject()
        val builder = readFields(reader, createObject())
        reader.endObject()
        return builder
    }

    private fun readFields(reader: JsonStringReader, builder: T): T {
        var updateObject = builder
        while (reader.hasNext()) {
            updateObject = updateObject(reader, updateObject)
            reader.next()
        }
        return updateObject
    }

    abstract fun createObject(): T

    abstract fun updateObject(reader: JsonStringReader, builder: T): T
}
