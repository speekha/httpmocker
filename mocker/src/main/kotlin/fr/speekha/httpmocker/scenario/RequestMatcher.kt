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

package fr.speekha.httpmocker.scenario

import fr.speekha.httpmocker.io.matchBody
import fr.speekha.httpmocker.model.RequestDescriptor
import okhttp3.Request

class RequestMatcher {

    fun matchRequest(descriptor: RequestDescriptor, request: Request): Boolean = with(descriptor) {
        matchProtocol(request) &&
                matchMethod(request) &&
                matchHost(request) &&
                matchPort(request) &&
                matchPath(request) &&
                matchHeaders(request) &&
                matchParams(request) &&
                matchBody(request)
    }

    private fun RequestDescriptor.matchBody(request: Request) =
        request.matchBody(this)

    private fun RequestDescriptor.matchParams(request: Request) =
        params.all {
            request.url().queryParameter(it.key) == it.value
        } && (!exactMatch || params.size == request.url().querySize())

    private fun RequestDescriptor.matchHeaders(request: Request) =
        headers.all {
            if (it.value != null) {
                request.headers(it.name).contains(it.value)
            } else {
                request.headers(it.name).isEmpty()
            }
        } && (!exactMatch || headers.size == request.headers().size())

    private fun RequestDescriptor.matchPath(request: Request) =
        path?.let { it == request.url().encodedPath() } ?: true

    private fun RequestDescriptor.matchPort(request: Request) =
        port?.let { it == request.url().port() } ?: true

    private fun RequestDescriptor.matchHost(request: Request) =
        host?.equals(request.url().host(), true) ?: true

    private fun RequestDescriptor.matchMethod(request: Request) =
        method?.equals(request.method(), true) ?: true

    private fun RequestDescriptor.matchProtocol(request: Request) =
        protocol?.equals(request.url().scheme(), true) ?: true
}
