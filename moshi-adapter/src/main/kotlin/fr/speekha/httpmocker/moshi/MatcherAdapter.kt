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


class MatcherAdapter {
    @FromJson
    fun matcherFromJson(matcher: JsonMatcher): Matcher {
        return Matcher(requestFromJson(matcher.request), responseFromJson(matcher.response))
    }

    @ToJson
    fun eventToJson(matcher: Matcher): JsonMatcher {
        return JsonMatcher(requestToJson(matcher.request), responseToJson(matcher.response))
    }

    fun requestFromJson(request: JsonRequestDescriptor?) = request?.run {
        RequestDescriptor(method, headers.map { headerFromJson(it) }, params, body)
    } ?: RequestDescriptor()

    fun requestToJson(request: RequestDescriptor) =
        JsonRequestDescriptor(request.method, request.headers.map { headerToJson(it) }, request.params, request.body)

    fun responseFromJson(response: JsonResponseDescriptor) = ResponseDescriptor(
        response.delay,
        response.code,
        response.mediaType,
        response.headers.map { headerFromJson(it) },
        response.body,
        response.bodyFile
    )

    fun responseToJson(response: ResponseDescriptor) =
        JsonResponseDescriptor(
            response.delay,
            response.code,
            response.mediaType,
            response.headers.map { headerToJson(it) },
            response.body,
            response.bodyFile
        )


    fun headerFromJson(header: JsonHeader): Header = Header(header.name, header.value)

    fun headerToJson(header: Header) = JsonHeader(header.name, header.value)
}