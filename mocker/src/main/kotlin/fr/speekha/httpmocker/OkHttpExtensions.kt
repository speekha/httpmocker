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

package fr.speekha.httpmocker

import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer

/**
 * Reads a request body and returns it as a String
 * @return a string with the request body content
 */
internal fun RequestBody.readAsString(): String? = Buffer().let {
    writeTo(it)
    it.inputStream().bufferedReader().use { reader -> reader.readText() }
}

/**
 * Tries to match an OkHttp Request with a request template
 * @return true if the request matches the template, false if it doesn't
 */
internal fun Request.matchBody(request: RequestDescriptor): Boolean = request.body?.let { bodyPattern ->
    val requestBody = body()?.readAsString()
    requestBody != null && Regex(bodyPattern).matches(requestBody)
} ?: true

/**
 * Converts an OkHttp Request to a template
 * @return the request description
 */
internal fun Request.toDescriptor() = RequestDescriptor(
    method = method(),
    body = body()?.readAsString(),
    params = url().queryParameterNames().associate { it to (url().queryParameter(it) ?: "") },
    headers = headers().names().flatMap { name -> headers(name).map { Header(name, it) } }
)

/**
 * Converts an OkHttp Response to a mock entry
 * @return the response description
 */
internal fun Response.toDescriptor(duplicates: Int, extension: String?) = ResponseDescriptor(
    code = code(),
    bodyFile = extension?.let {
        request().url().toBodyFile() + "_body_$duplicates$it"
    },
    headers = headers().names().flatMap { name -> headers(name).map { Header(name, it) } }
)

private fun HttpUrl.toBodyFile() = pathSegments().last().takeUnless { it.isNullOrBlank() } ?: "index"

/**
 * Duplicates a response and creates a new body for the duplicate (response body is a stream and
 * can only be read once)
 * @param body the response body to include
 * @return the copy of the original response
 */
internal fun Response.copyResponse(body: ByteArray?): Response = newBuilder()
    .body(ResponseBody.create(body()?.contentType(), body ?: byteArrayOf()))
    .build()
