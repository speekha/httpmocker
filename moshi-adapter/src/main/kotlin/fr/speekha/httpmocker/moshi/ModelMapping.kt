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

import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestTemplate
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.moshi.model.Header as JsonHeader
import fr.speekha.httpmocker.moshi.model.NetworkError as JsonNetworkError
import fr.speekha.httpmocker.moshi.model.RequestDescriptor as JsonRequestDescriptor
import fr.speekha.httpmocker.moshi.model.ResponseDescriptor as JsonResponseDescriptor

internal fun JsonRequestDescriptor.fromJson() = RequestTemplate(
    exactMatch ?: false, protocol, method, host, port, path,
    headers.map { it.fromJson() }, params, body
)

internal fun RequestTemplate.toJson() = JsonRequestDescriptor(
    exactMatch.takeIf { it }, protocol, method, host, port, path,
    headers.map { it.toJson() }, params, body
)

internal fun JsonResponseDescriptor.fromJson() = ResponseDescriptor(
    delay, code, mediaType, headers.map { it.fromJson() }, body, bodyFile
)

internal fun ResponseDescriptor.toJson() = JsonResponseDescriptor(
    delay, code, mediaType, headers.map { it.toJson() }, body, bodyFile
)

internal fun JsonNetworkError.fromJson() = NetworkError(
    exceptionType, message
)

internal fun NetworkError.toJson() = JsonNetworkError(
    exceptionType, message
)

private fun JsonHeader.fromJson(): Header = Header(name, value)

private fun Header.toJson() = JsonHeader(name, value)
