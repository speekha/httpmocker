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

fun compactJson(json: String): String =
    json.split("\n").joinToString("") { it.trim() }

fun List<Matcher>.toJson() =
    joinToString(separator = ", ", prefix = "[\n", postfix = "]") { it.toJson() }

fun Matcher.toJson(): String = """  {
    "request": ${request.toJson()},
    "response": ${response.toJson()}
  }
"""

fun RequestDescriptor.toJson(): String = listOf(
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

fun <K, V> Map<K, V>.toJson(): String =
    entries.joinToString(
        separator = ", ",
        prefix = "{\n",
        postfix = "\n      }"
    ) { "        \"${it.key}\": \"${it.value}\"" }

fun Header.toJson(): String = "\"$name\": \"$value\""

fun ResponseDescriptor.toJson(): String = listOf(
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

private fun String?.wrap() = this?.let { "\"$it\"" }

private fun Int?.wrap() = this?.toString()

fun String.truncate(limit: Int): String = takeIf { length <= limit } ?: substring(0, limit - 3) + "..."