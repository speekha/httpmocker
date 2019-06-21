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

import java.util.regex.Pattern

class JsonStringReader(
    input: String
) {

    private val json: String = compactJson(input)

    private var index = 0

    fun hasNext(): Boolean {
        return index < json.length && json[index] != '}' && json[index] != ']'
    }

    fun next() {
        val comma = json.indexOf(',', index) + 1
        val brace = json.indexOf('}', index)
        val bracket = json.indexOf(']', index)
        index = listOf(comma, brace, bracket).filter { it >= index }.min() ?: index
    }

    fun beginObject() {
        when {
            index >= json.length -> error(END_OF_STRING_ERROR)
            json[index] != '{' -> parseError(WRONG_START_OF_OBJECT_ERROR)
            else -> index++
        }
    }

    fun endObject() {
        val brace = json.indexOf('}', index)
        if (!json.substring(index, brace).isBlank()) {
            parseError(WRONG_END_OF_OBJECT_ERROR)
        }
        index = brace + 1
    }

    fun beginList() {
        when {
            index >= json.length -> error(END_OF_STRING_ERROR)
            json[index] != '[' -> parseError(WRONG_START_OF_LIST_ERROR)
            else -> index++
        }
    }

    fun endList() {
        val bracket = json.indexOf(']', index)
        if (!json.substring(index, bracket).isBlank()) {
            parseError(WRONG_END_OF_LIST_ERROR)
        }
        index = bracket + 1
    }

    fun readFieldName(): String {
        val backupIndex = index
        val stringLitteral = extractStringLitteral()
        val colon = json.indexOf(":", index) + 1
        if (colon <= index || !isFieldSeparator(index, colon)) {
            index = backupIndex
            parseError(NO_FIELD_ID_ERROR)
        } else {
            index = colon
            return stringLitteral
        }
    }

    fun readInt(): Int = extractNumericLitteral().toInt()

    fun readLong(): Long = extractNumericLitteral().toLong()

    fun readString(): String {
        val start = json.indexOf("\"", index)
        if (start < index || !isBlank(index, start)) {
            parseError(WRONG_START_OF_STRING_FIELD_ERROR)
        } else {
            index = start
        }
        return extractStringLitteral()
    }


    fun <T : Any> readObject(adapter: ObjectAdapter<T>): T {
        val brace = json.indexOf('{', index)
        if (!isBlank(index, brace)) {
            parseError(WRONG_START_OF_OBJECT_ERROR)
        }
        index = brace
        return adapter.fromJson(this)
    }

    private fun extractStringLitteral(): String {
        val start = json.indexOf("\"", index)
        val end = json.indexOf("\"", start + 1)
        if (start < 1 || end == -1 || !isBlank(index, start)) {
            parseError(WRONG_START_OF_STRING_ERROR)
        }
        index = end + 1
        return json.substring(start + 1, end)
    }

    private fun extractNumericLitteral(): String {
        val pattern = Pattern.compile("\\d[\\d ]*")
        val matcher = pattern.matcher(json.substring(index))
        if (!matcher.find() || !isBlank(index, index + matcher.start())) {
            parseError(INVALID_NUMBER_ERROR)
        }
        index += matcher.end()
        return matcher.group().replace(" ", "")
    }

    private fun isFieldSeparator(start: Int, end: Int) = json.substring(start, end).trim() == ":"

    private fun isBlank(start: Int, end: Int) = json.substring(start, end).isBlank()

    private fun parseError(message: String): Nothing = error("$message${extractAfterCurrentPosition()}")

    private fun extractAfterCurrentPosition() = json.substring(index).truncate(10)
}

const val END_OF_STRING_ERROR = "Reached end of object"
const val WRONG_START_OF_OBJECT_ERROR = "No object starts here: "
const val NO_FIELD_ID_ERROR = "No field starts here: "
const val WRONG_END_OF_OBJECT_ERROR = "Object is not entirely processed: "
const val WRONG_START_OF_LIST_ERROR = "No list starts here: "
const val WRONG_END_OF_LIST_ERROR = "List is not entirely processed: "
const val WRONG_START_OF_STRING_ERROR = "No string starts here: "
const val WRONG_START_OF_STRING_FIELD_ERROR = "Not ready to read a string value for a field: "
const val INVALID_NUMBER_ERROR = "Invalid numeric value: "

