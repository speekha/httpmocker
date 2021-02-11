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

import fr.speekha.httpmocker.custom.serializer.truncate

internal class StringReader(
    val json: String
) {

    private var index = 0

    fun parseError(message: String): Nothing =
        error("$message${extractAfterCurrentPosition(index)}")

    fun skip(length: Int) {
        index += length
    }

    fun skipBlanks() {
        while (index < json.length && json[index].isWhitespace()) {
            index++
        }
    }

    fun hasTokensLeft() = index < json.length

    fun isCurrentCharacter(vararg characters: Char) = json[index] in characters

    fun extractLiteral(pattern: Regex, error: String = INVALID_TOKEN_ERROR): String {
        val find = pattern.find(json.substring(index))
        val range = find?.range
        if (range == null || isNotBlank(index, index + range.first)) {
            parseError(error)
        }
        index += range.last + 1
        return find.value
    }

    private fun extract(start: Int, end: Int) = json.substring(start, end).trim()

    private fun extractAfterCurrentPosition(position: Int) =
        json.substring(position).truncate(DEFAULT_TRUCATE_LENGTH)

    private fun isNotBlank(start: Int, end: Int) = extract(start, end).isNotEmpty()

    fun moveToNextToken(token: Char, error: String) {
        val tokenPosition = findNextToken(token, error)
        index = tokenPosition
    }

    fun passNextToken(token: Char, error: String) {
        val tokenPosition = findNextToken(token, error)
        index = tokenPosition + 1
    }

    private fun findNextToken(token: Char, error: String): Int = next(token).also {
        if (it < index || isNotBlank(index, it)) {
            parseError(error)
        }
    }

    fun moveToFirstAvailableToken(error: String, vararg tokens: Char) {
        index = tokens
            .map { next(it) }
            .filter { it >= index && it < json.length }
            .min() ?: parseError(error)
    }

    private fun next(c: Char) = json.indexOf(c, index)
}
