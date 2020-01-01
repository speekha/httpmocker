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
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.serialization.EXCEPTION_MESSAGE
import fr.speekha.httpmocker.serialization.EXCEPTION_TYPE

internal class ErrorAdapter : BaseObjectAdapter<NetworkError>() {

    override fun createObject(): NetworkError = NetworkError()

    override fun updateObject(
        reader: JsonParser,
        builder: NetworkError
    ): NetworkError = when (val field = reader.readFieldName()) {
        EXCEPTION_TYPE -> builder.copy(exceptionType = reader.readString() ?: "")
        EXCEPTION_MESSAGE -> builder.copy(message = reader.readString() ?: "")
        else -> unknownFieldError(field)
    }
}
