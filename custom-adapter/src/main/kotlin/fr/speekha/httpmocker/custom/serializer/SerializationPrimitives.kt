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
import fr.speekha.httpmocker.custom.parser.ELLIPSIS
import fr.speekha.httpmocker.custom.parser.OPENING_BRACE

internal fun writeObjectFields(level: Int, vararg pairs: Pair<String, Any?>) =
    pairs.filter { it.second != null }
        .joinToString(
            separator = COMMA,
            prefix = OPENING_BRACE,
            postfix = closingBrace(level)
        ) { writePair(level + 1, it) }

internal fun String?.wrap() = this?.let { "\"${it.replace("\"", "\\\"")}\"" }

internal fun Int?.wrap() = this?.toString()

internal fun writePair(indent: Int, pair: Pair<String, Any?>): String {
    val (key, value) = pair
    return "${" ".repeat(indent * 2)}\"$key\": $value"
}

internal fun closingBrace(indent: Int) = "\n${" ".repeat(indent * 2)}}"

/**
 * Truncates a string and adds ... to show that the String is incomplete
 * @param limit the maximum length for the String
 * @return the truncated version of the String
 */
fun String.truncate(limit: Int): String =
    takeIf { length <= limit } ?: substring(0, limit - ELLIPSIS.length) + ELLIPSIS
