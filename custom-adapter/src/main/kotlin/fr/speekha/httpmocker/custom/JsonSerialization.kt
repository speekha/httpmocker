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

fun List<Matcher>.toJson() = joinToString(separator = ", ", prefix = "[", postfix = "]") { it.toJson() }

fun Matcher.toJson(): String = """{
    "request": ${request.toJson()},
    "response": ${response.toJson()}
  }"""

fun RequestDescriptor.toJson(): String = """{
    "method": "$method",
    "headers": {${headers.joinToString(separator = ",") { it.toJson() }}},
    "params": ${params.toJson()},
    "body": "$body"
  }"""

fun <K, V> Map<K, V>.toJson(): String =
    entries.joinToString(separator = ", ", prefix = "{", postfix = "}") { "\"${it.key}\": \"${it.value}\"" }

fun Header.toJson(): String = "\"$name\": \"$value\""

fun ResponseDescriptor.toJson(): String = """{
    "delay": $delay,
    "code": $code,
    "media-type": "$mediaType",
    "headers": {${headers.joinToString(separator = ",") { it.toJson() }}},
    "body": "$body",
    "body-file": "$bodyFile"
  }"""


fun String.truncate(limit: Int): String {
    return if (length > limit) {
        substring(0, limit-3) + "..."
    } else {
        this
    }
}