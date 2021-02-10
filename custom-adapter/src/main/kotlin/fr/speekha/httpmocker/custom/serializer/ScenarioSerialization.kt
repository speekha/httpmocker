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

package fr.speekha.httpmocker.custom.serializer

import fr.speekha.httpmocker.custom.parser.COMMA
import fr.speekha.httpmocker.custom.parser.OPENING_BRACE
import fr.speekha.httpmocker.model.*
import fr.speekha.httpmocker.serialization.*

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

internal fun RequestTemplate.toJson(indent: Int): String = writeObjectFields(
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

internal fun List<NamedParameter>.toJson(indent: Int): String = joinToString(
    separator = COMMA,
    prefix = OPENING_BRACE,
    postfix = closingBrace(indent)
) {
    writePair(
        indent + 1,
        it.name to it.value.wrap()
    )
}

internal fun NamedParameter.toJson(): String = "\"$name\": ${value.wrap()}"
