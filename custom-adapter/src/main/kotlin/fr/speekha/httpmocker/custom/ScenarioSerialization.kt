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
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.serialization.BODY
import fr.speekha.httpmocker.serialization.BODY_FILE
import fr.speekha.httpmocker.serialization.CODE
import fr.speekha.httpmocker.serialization.DELAY
import fr.speekha.httpmocker.serialization.ERROR
import fr.speekha.httpmocker.serialization.EXACT_MATCH
import fr.speekha.httpmocker.serialization.EXCEPTION_MESSAGE
import fr.speekha.httpmocker.serialization.EXCEPTION_TYPE
import fr.speekha.httpmocker.serialization.HEADERS
import fr.speekha.httpmocker.serialization.HOST
import fr.speekha.httpmocker.serialization.MEDIA_TYPE
import fr.speekha.httpmocker.serialization.METHOD
import fr.speekha.httpmocker.serialization.PARAMS
import fr.speekha.httpmocker.serialization.PATH
import fr.speekha.httpmocker.serialization.PORT
import fr.speekha.httpmocker.serialization.PROTOCOL
import fr.speekha.httpmocker.serialization.REQUEST
import fr.speekha.httpmocker.serialization.RESPONSE

internal fun List<Matcher>.toJson() =
    joinToString(separator = ", ", prefix = "[\n  ", postfix = "\n]") { it.toJson(1) }

internal fun Matcher.toJson(indent: Int): String {
    val incrementIndent = indent + 1
    return writeObjectFields(
        indent,
        REQUEST to request.toJson(incrementIndent),
        RESPONSE to response?.toJson(incrementIndent),
        ERROR to error?.toJson(incrementIndent)
    )
}

internal fun RequestDescriptor.toJson(indent: Int): String = writeObjectFields(
    indent,
    EXACT_MATCH to exactMatch.takeIf { it },
    PROTOCOL to protocol.wrap(),
    METHOD to method.wrap(),
    HOST to host.wrap(),
    PORT to port.wrap(),
    PATH to path.wrap(),
    HEADERS to "{\n        ${headers.joinToString(separator = ",\n        ") { it.toJson() }}\n      }",
    PARAMS to params.toJson(indent + 1),
    BODY to body.wrap()
)

internal fun ResponseDescriptor.toJson(indent: Int): String = writeObjectFields(
    indent,
    DELAY to delay.toString(),
    CODE to code.toString(),
    MEDIA_TYPE to mediaType.wrap(),
    HEADERS to "{\n        ${headers.joinToString(separator = ",\n        ") { it.toJson() }}\n      }",
    BODY to body.wrap(),
    BODY_FILE to bodyFile.wrap()
)

internal fun NetworkError.toJson(indent: Int): String = writeObjectFields(
    indent,
    EXCEPTION_TYPE to exceptionType.wrap(),
    EXCEPTION_MESSAGE to message.wrap()
)

internal fun Map<String, String?>.toJson(indent: Int): String =
    entries.joinToString(
        separator = COMMA,
        prefix = OPENING_BRACE,
        postfix = closingBrace(indent)
    ) { writePair(indent + 1, it.key to it.value.wrap()) }

internal fun Header.toJson(): String = "\"$name\": ${value.wrap()}"
