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

package fr.speekha.httpmocker.custom.parser

import fr.speekha.httpmocker.custom.parser.adapters.ObjectAdapter

/**
 * A reader object to parse a JSON stream
 * @param json the string to parse
 */
class JsonParser(
    json: String
) {

    private val handler = StringReader(json)

    private val parser = SimpleTypeParser(handler)

    /**
     * Checks whether the string still has tokens to process
     * @return false if the unit being parsed has been completely processed, true if it still
     * contains elements
     */
    fun hasNext(): Boolean {
        handler.skipBlanks()
        return handler.hasTokensLeft() && !handler.isCurrentCharacter('}', ']')
    }

    /**
     * Moves to the next element in an object or a list
     */
    fun next() {
        handler.moveToFirstAvailableToken(NO_MORE_TOKEN_ERROR, ',', '}', ']')
        if (handler.isCurrentCharacter(',')) {
            handler.skip(1)
        }
    }

    /**
     * Processes the beginning of an object
     */
    fun beginObject() {
        handler.passNextToken('{', WRONG_START_OF_OBJECT_ERROR)
    }

    /**
     * Processes the end of an object
     */
    fun endObject() {
        handler.passNextToken('}', WRONG_END_OF_OBJECT_ERROR)
    }

    /**
     * Reads an object field value
     * @return the field value as an object
     */
    fun <T : Any> readObject(adapter: ObjectAdapter<T>): T {
        handler.moveToNextToken('{', WRONG_START_OF_OBJECT_ERROR)
        return adapter.fromJson(this)
    }

    /**
     * Processes the beginning of a list
     */
    fun beginList() {
        handler.passNextToken('[', WRONG_START_OF_LIST_ERROR)
    }

    /**
     * Processes the end of a list
     */
    fun endList() {
        handler.passNextToken(']', WRONG_END_OF_LIST_ERROR)
    }

    /**
     * Reads the name of a JSON field
     * @return the name of the current field as a String
     */
    fun readFieldName(): String = parser.readString(WRONG_START_OF_FIELD_NAME_ERROR).also {
        handler.passNextToken(':', INCORRECT_FIELD)
    }

    /**
     * Reads an Integer field value
     * @return the field value as an Integer
     */
    fun readInt(): Int = parser.readNumeric(String::toInt)

    /**
     * Reads a Long field value
     * @return the field value as a Long
     */
    fun readLong(): Long = parser.readNumeric(String::toLong)

    /**
     * Reads a Boolean field value
     * @return the field value as a Boolean
     */
    fun readBoolean(): Boolean = parser.readBoolean()

    /**
     * Reads a String field value
     * @return the field value as a String
     */
    fun readString(): String? = parser.readStringOrNull(WRONG_START_OF_STRING_FIELD_ERROR)
}
