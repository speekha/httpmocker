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

package fr.speekha.httpmocker.kotlinx

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Headers are generally serialized as a map of String keys and values, but duplicate keys are
 * possible (for instance "Set-Cookie" or "Cookie" header can occur several times with different
 * values in the same request or response), which is not supported by Kotlinx serialization.
 * This class llows to convert the JSON format supported by other modules to a format compatible
 * with Kotlinx serialization.
 * If this class is not used, scenarios will have to match the following format for headers:
 * <code>
 * [
 *   {
 *     "request": {
 *       "headers": [
 *         {
 *           "name": "header1",
 *           "value": "value1"
 *         }
 *       ]
 *     },
 *     "response": {
 *       "headers": [
 *         {
 *           "name": "header1",
 *           "value": "value1"
 *         }
 *       ]
 *     }
 *   }
 *  ]
 * </code>
 */
class JsonFormatConverter {

    /**
     * Converts Kotlinx compatible JSON to common JSON format.
     */
    fun export(json: String): String = StringBuilder().apply {
        val matcher = outputHeaderPattern.matcher(json)
        var position = 0
        while (matcher.find()) {
            exportHeaderBlock(json, matcher, position)
            position = matcher.end()
        }
        append(json.substring(position until json.length))
    }.toString()

    private fun StringBuilder.exportHeaderBlock(json: String, matcher: Matcher, startAt: Int) {
        val openingBracket = json.indexOf("[", matcher.start())
        append(json.substring(startAt until openingBracket))
        val headers =
            mapOutputHeaders(json.substring(openingBracket + 1 until matcher.end() - 1))
        append("{$headers}")
    }

    private fun mapOutputHeaders(headers: String) =
        headers
            .replace(Regex("\\{\\p{Space}*\"name\"\\p{Space}*:\\p{Space}*"), "")
            .replace(Regex(",\\p{Space}*\"value\""), "")
            .replace(Regex("\\p{Space}*}\\p{Blank}*"), "")

    /**
     * Converts common format JSON to Kotlinx compatible one.
     */
    fun import(json: String): String = StringBuilder().apply {
        var position = 0
        val matcher = inputHeaderPattern.matcher(json)
        while (matcher.find()) {
            importHeaderBlock(json, matcher, position)
            position = matcher.end()
        }
        append(json.substring(position until json.length))
    }.toString()

    private fun StringBuilder.importHeaderBlock(json: String, matcher: Matcher, startAt: Int) {
        val openingBrace = json.indexOf("{", matcher.start())
        append(json.substring(startAt until openingBrace))
        append(mapInputHeaders(json.substring(openingBrace + 1 until matcher.end() - 1)))
    }

    private fun mapInputHeaders(headers: String): String =
        headers.split(",").filter { it.isNotBlank() }.joinToString(
            separator = ",",
            prefix = "[",
            postfix = "\n      ]",
            transform = ::mapInputHeader
        )

    private fun mapInputHeader(header: String): String = with(separatorPattern.matcher(header)) {
        if (find()) {
            val key = header.substring(0..start()).trim()
            val value = header.substring(end() - 1).trim()
            """
            |
            |        {
            |          "name": $key,
            |          "value": $value
            |        }""".trimMargin()
        } else {
            error("Invalid JSON")
        }
    }

    companion object {
        private val outputHeaderPattern =
            Pattern.compile("\"headers\"\\p{Space}*:\\p{Space}*\\[[^]]*]")
        private val inputHeaderPattern =
            Pattern.compile("\"headers\"\\p{Space}*:\\p{Space}*\\{[^}]*}")
        private val separatorPattern = Pattern.compile("\"\\p{Space}*:\\p{Space}*\"")
    }
}
