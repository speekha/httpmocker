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

package fr.speekha.httpmocker.gson

import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestTemplate
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.gson.model.Header as JsonHeader
import fr.speekha.httpmocker.gson.model.Matcher as JsonMatcher
import fr.speekha.httpmocker.gson.model.NetworkError as JsonNetworkError
import fr.speekha.httpmocker.gson.model.RequestDescriptor as JsonRequestDescriptor
import fr.speekha.httpmocker.gson.model.ResponseDescriptor as JsonResponseDescriptor

internal fun JsonMatcher.toModel() =
    Matcher(request?.toModel() ?: RequestTemplate(), response?.toModel(), error?.toModel())

private fun JsonRequestDescriptor.toModel() = RequestTemplate(
    exactMatch ?: false, protocol, method, host, port, path,
    headers.toModel(), params, body
)

private fun JsonHeader.toModel() = NamedParameter(name, value)

private fun HeaderAdapter.HeaderList?.toModel() = this?.map { it.toModel() } ?: emptyList()

private fun JsonResponseDescriptor.toModel() = ResponseDescriptor(
    delay, code, mediaType, headers.toModel(), body, bodyFile
)

private fun JsonNetworkError.toModel() = NetworkError(exceptionType, message)

internal fun Matcher.fromModel() =
    JsonMatcher(request.fromModel(), response?.fromModel(), error?.fromModel())

private fun RequestTemplate.fromModel() = JsonRequestDescriptor(
    exactMatch.takeIf { it }, protocol, method, host, port, path,
    getHeaders(), ParamsAdapter.ParamList(params), body
)

private fun RequestTemplate.getHeaders() =
    HeaderAdapter.HeaderList(headers.map { it.fromModel() })

private fun NamedParameter.fromModel() = JsonHeader(name, value)

private fun ResponseDescriptor.fromModel() = JsonResponseDescriptor(
    delay,
    code,
    mediaType,
    HeaderAdapter.HeaderList().apply {
        addAll(headers.map { it.fromModel() })
    },
    body,
    bodyFile
)

private fun NetworkError.fromModel() = JsonNetworkError(exceptionType, message)
