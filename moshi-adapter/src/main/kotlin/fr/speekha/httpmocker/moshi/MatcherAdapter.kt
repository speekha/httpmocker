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
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.moshi.Header as JsonHeader
import fr.speekha.httpmocker.moshi.Matcher as JsonMatcher
import fr.speekha.httpmocker.moshi.RequestDescriptor as JsonRequestDescriptor
import fr.speekha.httpmocker.moshi.ResponseDescriptor as JsonResponseDescriptor

internal class MatcherAdapter {
    @FromJson
    fun matcherFromJson(matcher: JsonMatcher): Matcher =
        Matcher(requestFromJson(matcher.request), responseFromJson(matcher.response))

    @ToJson
    fun matcherToJson(matcher: Matcher): JsonMatcher =
        JsonMatcher(requestToJson(matcher.request), responseToJson(matcher.response))

    private fun requestFromJson(request: JsonRequestDescriptor) = with(request) {
        RequestDescriptor(
            exactMatch ?: false, protocol, method, host, port, path,
            headers.map { headerFromJson(it) }, params, body
        )
    }

    private fun requestToJson(request: RequestDescriptor) = with(request) {
        JsonRequestDescriptor(
            exactMatch.takeIf { it }, protocol, method, host, port, path,
            headers.map { headerToJson(it) }, params, body
        )
    }

    private fun responseFromJson(response: JsonResponseDescriptor) = with(response) {
        ResponseDescriptor(
            delay, code, mediaType, headers.map { headerFromJson(it) }, body, bodyFile
        )
    }

    private fun responseToJson(response: ResponseDescriptor) = with(response) {
        JsonResponseDescriptor(
            delay, code, mediaType, headers.map { headerToJson(it) }, body, bodyFile
        )
    }

    private fun headerFromJson(header: JsonHeader): Header = Header(header.name, header.value)

    private fun headerToJson(header: Header) = JsonHeader(header.name, header.value)
}
