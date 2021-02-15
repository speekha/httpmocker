/*
 * Copyright 2019-2021 David Blanc
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
import fr.speekha.httpmocker.model.RequestTemplate
import fr.speekha.httpmocker.serialization.BODY
import fr.speekha.httpmocker.serialization.EXACT_MATCH
import fr.speekha.httpmocker.serialization.HEADERS
import fr.speekha.httpmocker.serialization.HOST
import fr.speekha.httpmocker.serialization.METHOD
import fr.speekha.httpmocker.serialization.PARAMS
import fr.speekha.httpmocker.serialization.PATH
import fr.speekha.httpmocker.serialization.PORT
import fr.speekha.httpmocker.serialization.PROTOCOL

internal class RequestAdapter : BaseObjectAdapter<RequestTemplate>() {

    override fun createObject(): RequestTemplate = RequestTemplate()

    @Suppress("ComplexMethod")
    override fun updateObject(
        reader: JsonParser,
        builder: RequestTemplate
    ): RequestTemplate = when (val field = reader.readFieldName()) {
        EXACT_MATCH -> builder.copy(exactMatch = reader.readBoolean())
        PROTOCOL -> builder.copy(protocol = reader.readString())
        METHOD -> builder.copy(method = reader.readString())
        PORT -> builder.copy(port = reader.readInt())
        HOST -> builder.copy(host = reader.readString())
        PATH -> builder.copy(path = reader.readString())
        HEADERS -> builder.copy(headers = reader.readObject(ParamListAdapter()))
        PARAMS -> builder.copy(params = reader.readObject(ParamListAdapter()))
        BODY -> builder.copy(body = reader.readString())
        else -> unknownFieldError(field)
    }
}
