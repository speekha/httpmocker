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

package fr.speekha.httpmocker.custom.parser

import java.util.*

internal class SimpleTypeParser(
    private val handler: StringReader
) {

    fun readString(error: String): String = parseToken(stringPattern, error) {
        parseStringLiteral(it.trim(), error) ?: handler.parseError("$error $NULL_STRING_VALUE")
    }

    fun readStringOrNull(error: String): String? = parseToken(stringPattern, error) {
        parseStringLiteral(it.trim(), error)
    }

    fun <T : Number> readNumeric(convert: String.() -> T): T =
        parseToken(numericPattern, INVALID_NUMBER_ERROR) {
            it.replace(" ", "").convert()
        }

    fun readBoolean(): Boolean =
        parseToken(alphanumericPattern, INVALID_BOOLEAN_ERROR, this::parseBoolean)

    private fun parseStringLiteral(value: String, error: String): String? = when {
        "null" == value -> null
        value.startsWith('"') && value.endsWith('"') -> decodeString(value)
        else -> handler.parseError(error)
    }

    private fun decodeString(value: String): String = value.drop(1)
        .dropLast(1)
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")

    private fun parseBoolean(value: String): Boolean = when (value.toLowerCase(Locale.ROOT)) {
        "true" -> true
        "false" -> false
        else -> error(INVALID_BOOLEAN_ERROR)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <T : Any?> parseToken(pattern: Regex, error: String, converter: (String) -> T): T =
        converter(handler.extractLiteral(pattern, error))
}
