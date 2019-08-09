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

import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor

/**
 * Removes all unnecessary blanks from a JSON string.
 * @param json the JSON stream to clean
 * @return the JSON with all useless blanks removed
 */
internal fun compactJson(json: String): String =
    json.split("\n").joinToString("") { it.trim() }

internal fun List<Matcher>.toJson() =
    joinToString(separator = ", ", prefix = "[\n", postfix = "]") { it.toJson() }

internal fun Matcher.toJson(): String = """  {
    "request": ${request.toJson()},
    "response": ${response.toJson()}
  }
"""

internal fun RequestDescriptor.toJson(): String = listOf(
    "exact-match" to exactMatch.takeIf { it },
    "protocol" to protocol.wrap(),
    "method" to method.wrap(),
    "host" to host.wrap(),
    "port" to port.wrap(),
    "path" to path.wrap(),
    "headers" to "{${headers.joinToString(separator = ",") { it.toJson() }}}",
    "params" to params.toJson(),
    "body" to body.wrap()
)
    .filter { it.second != null }
    .joinToString(
        separator = ",\n",
        prefix = "{\n",
        postfix = "\n    }"
    ) { (key, value) -> "      \"$key\": $value" }

internal fun Map<String, String?>.toJson(): String =
    entries.joinToString(
        separator = ",\n",
        prefix = "{\n",
        postfix = "\n      }"
    ) { "        \"${it.key}\": ${it.value.wrap()}" }

internal fun Header.toJson(): String = "\"$name\": ${value.wrap()}"

internal fun ResponseDescriptor.toJson(): String = listOf(
    "delay" to delay.toString(),
    "code" to code.toString(),
    "media-type" to mediaType.wrap(),
    "headers" to "{${headers.joinToString(separator = ",") { it.toJson() }}}",
    "body" to body.wrap(),
    "body-file" to bodyFile.wrap()
)
    .filter { it.second != null }
    .joinToString(
        separator = ",\n",
        prefix = "{\n",
        postfix = "\n    }"
    ) { (key, value) -> "      \"$key\": $value" }

private fun String?.wrap() = this?.let { "\"${it.replace("\"", "\\\"")}\"" }

private fun Int?.wrap() = this?.toString()

/**
 * Truncates a string and adds ... to show that the String is incomplete
 * @param limit the maximum length for the String
 * @return the truncated version of the String
 */
fun String.truncate(limit: Int): String =
    takeIf { length <= limit } ?: substring(0, limit - 3) + "..."