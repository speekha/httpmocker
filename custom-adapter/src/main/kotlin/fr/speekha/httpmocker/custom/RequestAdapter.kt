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

import fr.speekha.httpmocker.BODY
import fr.speekha.httpmocker.EXACT_MATCH
import fr.speekha.httpmocker.HEADERS
import fr.speekha.httpmocker.HOST
import fr.speekha.httpmocker.METHOD
import fr.speekha.httpmocker.PARAMS
import fr.speekha.httpmocker.PATH
import fr.speekha.httpmocker.PORT
import fr.speekha.httpmocker.PROTOCOL
import fr.speekha.httpmocker.model.RequestDescriptor

internal class RequestAdapter : BaseObjectAdapter<RequestDescriptor>() {

    override fun createObject(): RequestDescriptor = RequestDescriptor()

    @SuppressWarnings("ComplexMethod")
    override fun updateObject(
        reader: JsonStringReader,
        builder: RequestDescriptor
    ): RequestDescriptor = when (val field = reader.readFieldName()) {
        EXACT_MATCH -> builder.copy(exactMatch = reader.readBoolean())
        PROTOCOL -> builder.copy(protocol = reader.readString())
        METHOD -> builder.copy(method = reader.readString())
        PORT -> builder.copy(port = reader.readInt())
        HOST -> builder.copy(host = reader.readString())
        PATH -> builder.copy(path = reader.readString())
        HEADERS -> builder.copy(headers = reader.readObject(HeaderListAdapter()))
        PARAMS -> builder.copy(params = reader.readObject(MapAdapter()))
        BODY -> builder.copy(body = reader.readString())
        else -> unknownFieldError(field)
    }
}
