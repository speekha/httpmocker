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

package fr.speekha.httpmocker

import fr.speekha.httpmocker.model.Matcher
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Mapper object allowing to load and save mock scenarios.
 */
interface Mapper : Parser {

    /**
     * Reads possible matches from a JSON input stream
     * @param stream the JSON data as an input stream
     * @return the corresponding data objects
     */
    fun readMatches(stream: InputStream): List<Matcher> = fromJson(stream.readAsString())

    /**
     * Reads possible matches from a JSON input stream
     * @param file the JSON data as a File
     * @return the corresponding data objects
     */
    fun readMatches(file: File): List<Matcher> = readMatches(FileInputStream(file))

    /**
     * Writes possible matches in JSON in an output stream
     * @param outputStream the stream in which the data will be saved
     * @param matchers the list of Matchers to serialize
     */
    fun writeValue(outputStream: OutputStream, matchers: List<Matcher>) = outputStream.use {
        it.write(toJson(matchers).toByteArray())
    }

}

/**
 * A JSON parser, allowing to convert lists of Matcher objects to JSON and back
 */
interface Parser {

    /**
     * Parses a JSON string as a list of Matchers
     * @param json the JSON string to parse
     * @return the corresponding list
     */
    fun fromJson(json: String): List<Matcher>

    /**
     * Serializes a list of matchers as a JSON stream
     * @param matchers the list of matchers to serialize
     * @return a JSON string representing the list
     */
    fun toJson(matchers: List<Matcher>): String
}