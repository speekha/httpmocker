/*
 * Copyright 2019-2020 David Blanc
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

package fr.speekha.httpmocker.ktor.engine

import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.io.MediaType
import fr.speekha.httpmocker.messageForHttpCode
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.ResponseDescriptor
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.utils.EmptyContent
import io.ktor.content.ByteArrayContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.util.toMap
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.ByteArrayOutputStream

internal fun HttpResponseData.toDescriptor(url: Url) = ResponseDescriptor(
    code = statusCode.value,
    bodyFile = url.toBodyFile() + "_body_",
    headers = headers.toModel()
)

internal fun mapRequest(request: HttpRequestData) = request.toModel()

internal fun HttpRequestData.toModel(): HttpRequest = HttpRequest(
    method = method.value,
    scheme = url.protocol.name,
    host = url.host,
    port = url.port,
    path = url.encodedPath,
    params = url.parameters.toMap().mapValues { it.value[0] },
    headers = headers.toModel(),
    body = readBody()
)

internal fun mapResponse(request: HttpRequestData, response: ResponseDescriptor): HttpResponseData =
    response.toModel()

private fun ResponseDescriptor.toModel() = HttpResponseData(
    statusCode = HttpStatusCode(code, messageForHttpCode(code)),
    requestTime = GMTDate(),
    headers = buildHeaders(),
    version = HttpProtocolVersion.HTTP_2_0,
    body = ByteReadChannel(body),
    callContext = Job() + Dispatchers.IO
)

private fun ResponseDescriptor.buildHeaders() = headersOf(*buildHeaderList().toTypedArray())

private fun ResponseDescriptor.buildHeaderList() = headers
    .groupBy { it.name }
    .mapValues { it.value.mapNotNull { header -> header.value } }
    .entries
    .map { it.key to it.value } +
    (HttpHeaders.ContentType to listOf(mediaType))

private fun Headers.toModel() = entries().flatMap { (key, values) ->
    values.map { value -> Header(key, value) }
}

private fun HttpRequestData.readBody() = when (val content = body) {
    is EmptyContent -> null
    is TextContent -> content.text
    is ByteArrayContent -> content.bytes().contentToString()
    else -> body.toString()
}

internal fun Url.toBodyFile(): String {
    val lastSegment = encodedPath.substringAfterLast('/')
    return lastSegment.takeUnless { it.isBlank() } ?: "index"
}

internal fun HttpResponseData.withBody(body: ByteArray?) = HttpResponseData(
    statusCode = statusCode,
    requestTime = requestTime,
    headers = headers,
    version = version,
    body = body?.let { ByteReadChannel(it, 0, it.size) } ?: ByteReadChannel.Empty,
    callContext = callContext
)

internal fun HttpResponseData.getMediaType(): MediaType? {
    val contentType = headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
    return contentType?.run { MediaType(contentType.contentType, contentType.contentSubtype) }
}

internal suspend fun HttpResponseData.readBody(): ByteArray? = when (val content = body) {
    is ByteReadChannel -> content.readFully()
    else -> content.toString().toByteArray()
}

suspend fun ByteReadChannel.readFully(): ByteArray {
    val buffer = ByteArrayOutputStream()
    while (!this.isClosedForRead) {
        val line = ByteArray(1024)
        when (val length = readAvailable(line, 0, 1024)) {
            1024 -> buffer.write(line, 0, 1024)
            in 0..1023 -> buffer.write(line, 0, length)
        }
    }
    return buffer.toByteArray()
}