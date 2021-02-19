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

package fr.speekha.httpmocker.ktor.io

import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.io.MediaType
import fr.speekha.httpmocker.messageForHttpCode
import fr.speekha.httpmocker.model.NamedParameter
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
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

internal expect val dispatcherIO: CoroutineDispatcher

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
    params = url.parameters.entries()
        .flatMap { (key, values) -> values.map { NamedParameter(key, it) } },
    headers = headers.toModel(),
    body = readBody()
)

internal fun ResponseDescriptor.toKtorRequest() = HttpResponseData(
    statusCode = HttpStatusCode(code, messageForHttpCode(code)),
    requestTime = GMTDate(),
    headers = buildHeaders(),
    version = HttpProtocolVersion.HTTP_2_0,
    body = ByteReadChannel(body),
    callContext = Job() + dispatcherIO
)

private fun ResponseDescriptor.buildHeaders() = headersOf(*buildHeaderList().toTypedArray())

private fun ResponseDescriptor.buildHeaderList() = headers
    .groupBy { it.name }
    .mapValues { it.value.mapNotNull { header -> header.value } }
    .entries
    .map { it.key to it.value } + (HttpHeaders.ContentType to listOf(mediaType))

private fun Headers.toModel() = entries().flatMap { (key, values) ->
    values.map { value -> NamedParameter(key, value) }
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

internal suspend fun HttpResponseData.readBody(): ByteArray = when (val content = body) {
    is ByteReadChannel -> content.readBytes()
    else -> content.toString().toByteArray()
}

private const val READ_CHANNEL_CHUNKS = 1024

internal suspend fun ByteReadChannel.readBytes(): ByteArray {
    val data = mutableListOf<ByteArray>()
    while (!isClosedForRead) {
        transferChunk()?.let { data += it }
    }
    return ByteArray(data.sumBy { it.size }) {
        data[it / READ_CHANNEL_CHUNKS][it % READ_CHANNEL_CHUNKS]
    }
}

private suspend fun ByteReadChannel.transferChunk(): ByteArray? = withContext(dispatcherIO) {
    val buffer = ByteArray(READ_CHANNEL_CHUNKS)
    when (val length = readAvailable(buffer, 0, READ_CHANNEL_CHUNKS)) {
        READ_CHANNEL_CHUNKS -> buffer
        0 -> null
        else -> buffer.copyOfRange(0, length)
    }
}
