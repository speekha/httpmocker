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

import java.util.Locale
import java.util.regex.Pattern

/**
 * A reader object to parse a JSON stream
 * @param json the string to parse
 */
class JsonStringReader(
    private val json: String
) {

    private var index = 0

    /**
     * Checks whether the string still has tokens to process
     * @return false if the unit being parsed has been completely processed, true if it still
     * contains elements
     */
    fun hasNext(): Boolean = index < json.length && json[index] != '}' && json[index] != ']'

    /**
     * Moves to the next element in an object or a list
     */
    fun next() {
        val comma = json.indexOf(',', index) + 1
        val brace = json.indexOf('}', index)
        val bracket = json.indexOf(']', index)
        index = listOf(comma, brace, bracket).filter { it >= index }.min() ?: index
    }

    /**
     * Processes the beginning of an object
     */
    fun beginObject() {
        val start = json.indexOf("{", index)
        if (start < index || !isBlank(index, start)) {
            parseError(WRONG_START_OF_OBJECT_ERROR)
        } else {
            index = start + 1
        }
    }

    /**
     * Processes the end of an object
     */
    fun endObject() {
        val brace = json.indexOf('}', index)
        if (!json.substring(index, brace).isBlank()) {
            parseError(WRONG_END_OF_OBJECT_ERROR)
        }
        index = brace + 1
    }

    /**
     * Processes the beginning of a list
     */
    fun beginList() {
        val start = json.indexOf("[", index)
        if (start < index || !isBlank(index, start)) {
            parseError(WRONG_START_OF_LIST_ERROR)
        } else {
            index = start + 1
        }
    }

    /**
     * Processes the end of a list
     */
    fun endList() {
        val bracket = json.indexOf(']', index)
        if (!json.substring(index, bracket).isBlank()) {
            parseError(WRONG_END_OF_LIST_ERROR)
        }
        index = bracket + 1
    }

    /**
     * Reads the name of a JSON field
     * @return the name of the current field as a String
     */
    fun readFieldName(): String {
        val backupIndex = index
        val stringLiteral = extractStringLiteral()
        val colon = json.indexOf(":", index) + 1
        if (colon <= index || !isFieldSeparator(index, colon)) {
            index = backupIndex
            parseError(NO_FIELD_ID_ERROR)
        } else {
            index = colon
            return stringLiteral
        }
    }

    /**
     * Reads an Integer field value
     * @return the field value as an Integer
     */
    fun readInt(): Int = parseNumeric(String::toInt)

    /**
     * Reads a Long field value
     * @return the field value as a Long
     */
    fun readLong(): Long = parseNumeric(String::toLong)

    /**
     * Reads a Boolean field value
     * @return the field value as a Boolean
     */
    fun readBoolean(): Boolean =
        parseToken(alphanumericPattern, INVALID_BOOLEAN_ERROR, this::parseBoolean)

    /**
     * Reads a String field value
     * @return the field value as a String
     */
    fun readString(): String {
        val start = json.indexOf("\"", index)
        if (start < index || !isBlank(index, start)) {
            parseError(WRONG_START_OF_STRING_FIELD_ERROR)
        } else {
            index = start
        }
        return extractStringLiteral()
    }

    /**
     * Reads an object field value
     * @return the field value as an object
     */
    fun <T : Any> readObject(adapter: ObjectAdapter<T>): T {
        val brace = json.indexOf('{', index)
        if (!isBlank(index, brace)) {
            parseError(WRONG_START_OF_OBJECT_ERROR)
        }
        index = brace
        return adapter.fromJson(this)
    }

    private fun <T : Number> parseNumeric(convert: String.() -> T): T =
        parseToken(numericPattern, INVALID_NUMBER_ERROR) {
            it.replace(" ", "").convert()
        }

    private fun <T : Any> parseToken(pattern: Pattern, error: String, converter: (String) -> T): T {
        val position = index
        return try {
            converter(extractLiteral(pattern, error))
        } catch (e: Throwable) {
            parseError(error, position)
        }
    }

    private fun parseBoolean(value: String): Boolean = when (value.toLowerCase(Locale.ROOT)) {
        "true" -> true
        "false" -> false
        else -> error(INVALID_BOOLEAN_ERROR)
    }

    private fun extractStringLiteral(): String {
        val start = json.indexOf("\"", index)
        val match = Regex("[^\\\\]\"").find(json, start)
        val end = match?.range?.endInclusive ?: -1
        if (start < 1 || end == -1 || !isBlank(index, start)) {
            parseError(WRONG_START_OF_STRING_ERROR)
        }
        index = end + 1
        return json.substring(start + 1, end).replace("\\\"", "\"")
    }

    private fun extractLiteral(pattern: Pattern, error: String = INVALID_TOKEN_ERROR): String {
        val matcher = pattern.matcher(json.substring(index))
        if (!matcher.find() || !isBlank(index, index + matcher.start())) {
            parseError(error)
        }
        index += matcher.end()
        return matcher.group()
    }

    private fun isFieldSeparator(start: Int, end: Int) = json.substring(start, end).trim() == ":"

    private fun isBlank(start: Int, end: Int) = json.substring(start, end).isBlank()

    private fun parseError(message: String, position: Int = index): Nothing =
        error("$message${extractAfterCurrentPosition(position)}")

    private fun extractAfterCurrentPosition(position: Int) = json.substring(position).truncate(10)

}

const val WRONG_START_OF_OBJECT_ERROR = "No object starts here: "
const val NO_FIELD_ID_ERROR = "No field starts here: "
const val WRONG_END_OF_OBJECT_ERROR = "Object is not entirely processed: "
const val WRONG_START_OF_LIST_ERROR = "No list starts here: "
const val WRONG_END_OF_LIST_ERROR = "List is not entirely processed: "
const val WRONG_START_OF_STRING_ERROR = "No string starts here: "
const val WRONG_START_OF_STRING_FIELD_ERROR = "Not ready to read a string value for a field: "
const val INVALID_NUMBER_ERROR = "Invalid numeric value: "
const val INVALID_TOKEN_ERROR = "Invalid token value: "
const val INVALID_BOOLEAN_ERROR = "Invalid boolean value: "

private val numericPattern = Pattern.compile("\\d[\\d ]*")
private val alphanumericPattern = Pattern.compile("[^,}\\]\\s]+")
