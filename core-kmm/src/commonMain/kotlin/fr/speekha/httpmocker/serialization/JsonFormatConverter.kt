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

package fr.speekha.httpmocker.serialization

/**
 * Headers are generally serialized as a map of String keys and values, since it is more compact
 * than using a list of specific objects for each header. But that implies that duplicate keys are
 * possible (for instance "Set-Cookie" or "Cookie" header can occur several times with different
 * values in the same request or response), which is not supported by Kotlinx serialization
 * (and can be marked as inappropriate JSON by some code analysis tools).
 * This class allows to convert the compact JSON format supported by most modules to a format
 * compatible with Kotlinx serialization.
 * If this class is not used, scenarios will have to match the following format for headers with
 * Kotlinx serialization:
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
 *
 * If the long format is preferred, the formatter can also be used with the other modules to force a
 * more Lint-friendly JSON format.
 */
class JsonFormatConverter {

    /**
     * Converts Kotlinx compatible JSON to common JSON format.
     */
    fun compact(input: String): String =
        convertJsonBlock(
            convertJsonBlock(
                input,
                outputHeaderPattern
            ) { json, matcher, position ->
                exportHeaderBlock(json, matcher, position)
            },
            outputParameterPattern
        ) { json, matcher, position ->
            exportHeaderBlock(json, matcher, position)
        }

    /**
     * Converts common format JSON to Kotlinx compatible one.
     */
    fun expand(input: String): String =
        convertJsonBlock(
            convertJsonBlock(
                input,
                inputParameterPattern
            ) { json, matcher, position ->
                importHeaderBlock(json, matcher, position)
            },
            inputHeaderPattern
        ) { json, matcher, position ->
            importHeaderBlock(json, matcher, position)
        }

    private fun convertJsonBlock(
        json: String,
        pattern: Regex,
        transform: StringBuilder.(json: String, matcher: MatchResult, position: Int) -> Unit
    ) = StringBuilder().apply {
        val matcher = pattern.findAll(json)
        var endPosition = 0
        matcher.forEach {
            transform(json, it, endPosition)
            endPosition = it.range.last + 1
        }
        append(json.substring(endPosition until json.length))
    }.toString()

    private fun StringBuilder.exportHeaderBlock(json: String, matcher: MatchResult, startAt: Int) {
        val openingBracket = json.indexOf("[", matcher.range.first)
        append(json.substring(startAt until openingBracket))
        val headers =
            mapOutputHeaders(json.substring(openingBracket + 1 until matcher.range.last))
        append("{$headers}")
    }

    private fun mapOutputHeaders(headers: String) = headers
        .replace(Regex("\\{\\p{Space}*\"name\"\\p{Space}*:\\p{Space}*"), "")
        .replace(Regex(",\\p{Space}*\"value\""), "")
        .replace(Regex("\\p{Space}*}\\p{Blank}*"), "")

    private fun StringBuilder.importHeaderBlock(json: String, matcher: MatchResult, startAt: Int) {
        val openingBrace = json.indexOf("{", matcher.range.first)
        append(json.substring(startAt until openingBrace))
        append(mapInputHeaders(json.substring(openingBrace + 1 until matcher.range.last - 1)))
    }

    private fun mapInputHeaders(headers: String): String =
        headers.split(",").filter { it.isNotBlank() }.joinToString(
            separator = ",",
            prefix = "[",
            postfix = "\n      ]",
            transform = ::mapInputHeader
        )

    private fun mapInputHeader(header: String): String = separatorPattern.find(header)?.range?.let {
        val key = header.substring(0..it.first).trim()
        val value = header.substring(it.last).trim()
        """
            |
            |        {
            |          "name": $key,
            |          "value": $value
            |        }""".trimMargin()
    } ?: error("Invalid JSON")

    companion object {
        private val outputHeaderPattern =
            Regex("\"headers\"\\p{Space}*:\\p{Space}*\\[[^]]*]")
        private val inputHeaderPattern =
            Regex("\"headers\"\\p{Space}*:\\p{Space}*\\{[^}]*}")
        private val outputParameterPattern =
            Regex("\"params\"\\p{Space}*:\\p{Space}*\\[[^]]*]")
        private val inputParameterPattern =
            Regex("\"params\"\\p{Space}*:\\p{Space}*\\{[^}]*}")
        private val separatorPattern = Regex("\"\\p{Space}*:\\p{Space}*")
    }
}
