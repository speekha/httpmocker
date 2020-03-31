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

package fr.speekha.httpmocker.jackson

import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.jackson.model.Header as JsonHeader
import fr.speekha.httpmocker.jackson.model.Matcher as JsonMatcher
import fr.speekha.httpmocker.jackson.model.NetworkError as JsonNetworkError
import fr.speekha.httpmocker.jackson.model.RequestDescriptor as JsonRequestDescriptor
import fr.speekha.httpmocker.jackson.model.ResponseDescriptor as JsonResponseDescriptor

internal fun Matcher.fromModel() =
    JsonMatcher(request.fromModel(), response?.fromModel(), error?.fromModel())

internal fun JsonMatcher.toModel() =
    Matcher(request.toModel(), response?.toModel(), error?.toModel())

private fun JsonRequestDescriptor.toModel() =
    RequestDescriptor(
        exactMatch ?: false,
        protocol,
        method,
        host,
        port,
        path,
        headers.map { it.toModel() },
        params,
        body
    )

private fun RequestDescriptor.fromModel() =
    JsonRequestDescriptor(
        exactMatch.takeIf { it },
        protocol,
        method,
        host,
        port,
        path,
        headers.map { it.fromModel() },
        params,
        body
    )

private fun JsonHeader.toModel() = Header(name, value)

private fun Header.fromModel() = JsonHeader(name, value)

private fun JsonResponseDescriptor.toModel() =
    ResponseDescriptor(delay, code, mediaType, headers.map { it.toModel() }, body, bodyFile)

private fun ResponseDescriptor.fromModel() =
    JsonResponseDescriptor(delay, code, mediaType, headers.map { it.fromModel() }, body, bodyFile)

private fun NetworkError.fromModel() = JsonNetworkError(exceptionType, message)

private fun JsonNetworkError.toModel() = NetworkError(exceptionType, message)
