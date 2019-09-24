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

package fr.speekha.httpmocker.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.moshi.Header as JsonHeader
import fr.speekha.httpmocker.moshi.Matcher as JsonMatcher
import fr.speekha.httpmocker.moshi.NetworkError as JsonNetworkError
import fr.speekha.httpmocker.moshi.RequestDescriptor as JsonRequestDescriptor
import fr.speekha.httpmocker.moshi.ResponseDescriptor as JsonResponseDescriptor

internal class MatcherAdapter {
    @FromJson
    fun matcherFromJson(matcher: JsonMatcher): Matcher = Matcher(
        matcher.request.fromJson(),
        matcher.response?.fromJson(),
        matcher.error?.fromJson()
    )

    @ToJson
    fun matcherToJson(matcher: Matcher): JsonMatcher = JsonMatcher(
        matcher.request.toJson(),
        matcher.response?.toJson(),
        matcher.error?.toJson()
    )

    private fun JsonRequestDescriptor.fromJson() = RequestDescriptor(
        exactMatch ?: false, protocol, method, host, port, path,
        headers.map { it.fromJson() }, params, body
    )

    private fun RequestDescriptor.toJson() = JsonRequestDescriptor(
        exactMatch.takeIf { it }, protocol, method, host, port, path,
        headers.map { it.toJson() }, params, body
    )

    private fun JsonResponseDescriptor.fromJson() = ResponseDescriptor(
        delay, code, mediaType, headers.map { it.fromJson() }, body, bodyFile
    )

    private fun ResponseDescriptor.toJson() = JsonResponseDescriptor(
        delay, code, mediaType, headers.map { it.toJson() }, body, bodyFile
    )

    private fun JsonNetworkError.fromJson() = NetworkError(
        exceptionType, message
    )

    private fun NetworkError.toJson() = JsonNetworkError(
        exceptionType, message
    )

    private fun JsonHeader.fromJson(): Header = Header(name, value)

    private fun Header.toJson() = JsonHeader(name, value)
}
