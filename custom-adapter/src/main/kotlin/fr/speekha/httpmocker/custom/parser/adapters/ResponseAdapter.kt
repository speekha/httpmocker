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

package fr.speekha.httpmocker.custom.parser.adapters

import fr.speekha.httpmocker.custom.parser.JsonParser
import fr.speekha.httpmocker.custom.unknownFieldError
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.serialization.*

internal class ResponseAdapter : BaseObjectAdapter<ResponseDescriptor>() {

    override fun createObject(): ResponseDescriptor = ResponseDescriptor()

    override fun updateObject(
        reader: JsonParser,
        builder: ResponseDescriptor
    ): ResponseDescriptor = when (val field = reader.readFieldName()) {
        DELAY -> builder.copy(delay = reader.readLong())
        CODE -> builder.copy(code = reader.readInt())
        MEDIA_TYPE -> builder.copy(mediaType = reader.readString() ?: "")
        HEADERS -> builder.copy(headers = reader.readObject(ParamListAdapter()))
        BODY -> builder.copy(body = reader.readString() ?: "")
        BODY_FILE -> builder.copy(bodyFile = reader.readString())
        else -> unknownFieldError(field)
    }
}
