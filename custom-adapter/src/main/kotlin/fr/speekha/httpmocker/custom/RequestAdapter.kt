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

import fr.speekha.httpmocker.model.RequestDescriptor

internal class RequestAdapter : BaseObjectAdapter<RequestDescriptor>() {

    override fun createObject(): RequestDescriptor = RequestDescriptor()

    override fun updateObject(
        reader: JsonStringReader,
        builder: RequestDescriptor
    ): RequestDescriptor = when (val field = reader.readFieldName()) {
        "method" -> builder.copy(method = reader.readString())
        "port" -> builder.copy(port = reader.readInt())
        "host" -> builder.copy(host = reader.readString())
        "path" -> builder.copy(path = reader.readString())
        "headers" -> builder.copy(headers = reader.readObject(HeaderListAdapter()))
        "params" -> builder.copy(params = reader.readObject(MapAdapter()))
        "body" -> builder.copy(body = reader.readString())
        else -> error("Unknown field $field")
    }
}