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

package fr.speekha.httpmocker.okhttp.io

import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/**
 * Reads a request body and returns it as a String
 * @return a string with the request body content
 */
internal fun RequestBody.readAsString(): String = Buffer().let {
    writeTo(it)
    it.inputStream().bufferedReader().use { reader -> reader.readText() }
}

/**
 * Converts an OkHttp Request to a template
 * @return the request description
 */
internal fun Request.toGenericModel() = HttpRequest(
    method = method,
    scheme = url.scheme,
    host = url.host,
    port = url.port,
    path = url.encodedPath,
    params = parseQueryParameters(),
    headers = headers.parseHeaders { headers(it) },
    body = body?.readAsString()
)

/**
 * Converts an OkHttp Response to a mock entry
 * @return the response description
 */
internal fun Response.toDescriptor() = ResponseDescriptor(
    code = code,
    bodyFile = request.url.toBodyFile() + "_body_",
    headers = headers.parseHeaders { headers(it) }
)

private fun Headers.parseHeaders(getHeaders: (String) -> List<String>) =
    names().flatMap { name -> getHeaders(name).map { NamedParameter(name, it) } }

private fun Request.parseQueryParameters() =
    url.queryParameterNames.flatMap { name ->
        url.queryParameterValues(name).map { NamedParameter(name, it) }
    }

private fun HttpUrl.toBodyFile() = pathSegments.last().takeUnless { it.isBlank() } ?: "index"

/**
 * Executes a request and returns the corresponding response
 */
internal fun Interceptor.Chain.execute() = proceed(request())

/**
 * Duplicates a response and creates a new body for the duplicate (response body is a stream and
 * can only be read once)
 * @param body the response body to include
 * @return the copy of the original response
 */
internal fun Response.copyResponse(body: ByteArray?): Response = newBuilder()
    .body((body ?: byteArrayOf()).toResponseBody(this.body?.contentType()))
    .build()
