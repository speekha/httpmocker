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

package fr.speekha.httpmocker.scenario

import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.model.RequestTemplate

class RequestMatcher {

    fun matchRequest(template: RequestTemplate, request: HttpRequest): Boolean = with(template) {
        matchProtocol(request) &&
            matchMethod(request) &&
            matchHost(request) &&
            matchPort(request) &&
            matchPath(request) &&
            matchHeaders(request) &&
            matchParams(request) &&
            matchBody(request)
    }

    private fun RequestTemplate.matchBody(request: HttpRequest) = body?.let { bodyPattern ->
        request.body != null && Regex(bodyPattern).matches(request.body)
    } ?: true

    private fun RequestTemplate.matchParams(request: HttpRequest) =
        params.all { param ->
            if (param.value != null) {
                request.params.any { it == param }
            } else {
                request.params.none { it.name == param.name }
            }
        } && (!exactMatch || params.size == request.params.size)

    private fun RequestTemplate.matchHeaders(request: HttpRequest) =
        headers.all { header ->
            if (header.value != null) {
                request.headers.any { it == header }
            } else {
                request.headers.none { it.name == header.name }
            }
        } && (!exactMatch || headers.size == request.headers.size)

    private fun RequestTemplate.matchPath(request: HttpRequest) =
        path?.let { it == request.path } ?: true

    private fun RequestTemplate.matchPort(request: HttpRequest) =
        port?.let { it == request.port } ?: true

    private fun RequestTemplate.matchHost(request: HttpRequest) =
        host?.equals(request.host, true) ?: true

    private fun RequestTemplate.matchMethod(request: HttpRequest) =
        method?.equals(request.method, true) ?: true

    private fun RequestTemplate.matchProtocol(request: HttpRequest) =
        protocol?.equals(request.scheme, true) ?: true
}
