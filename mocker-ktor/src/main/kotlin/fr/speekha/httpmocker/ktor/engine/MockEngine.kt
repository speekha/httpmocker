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

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.NO_RECORDER_ERROR
import fr.speekha.httpmocker.RECORD_NOT_SUPPORTED_ERROR
import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.io.MediaType
import fr.speekha.httpmocker.io.MockResponder
import fr.speekha.httpmocker.io.RequestWriter
import fr.speekha.httpmocker.messageForHttpCode
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.ResponseDescriptor
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.utils.EmptyContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.util.InternalAPI
import io.ktor.util.date.GMTDate
import io.ktor.util.toMap
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class MockEngine(
    override val config: MockEngineConfig,
) : HttpClientEngineBase("Mock Engine") {

    private val job = Job()

    override val dispatcher = Dispatchers.IO

    override val coroutineContext: CoroutineContext = job + dispatcher

    private val logger = getLogger()

    private val internalConf = config.buildConfig()

    private val responder = MockResponder(
        internalConf.providers,
        internalConf.simulatedDelay,
        this::mapRequest,
        this::mapResponse
    )

    private val writer: RequestWriter? = config.configBuilder.buildRecorder()

    private val forbidRecord: Boolean
        get() = writer == null

    /**
     * Enables to set the interception mode. @see fr.speekha.httpmocker.MockResponseInterceptor.Mode
     */
    var mode: Mode
        get() = internalConf.status
        set(value) {
            if (value == Mode.RECORD && forbidRecord) {
                error(NO_RECORDER_ERROR)
            } else {
                internalConf.status = value
            }
        }

    private fun mapResponse(request: HttpRequestData, response: ResponseDescriptor): HttpResponseData =
        HttpResponseData(
            statusCode = HttpStatusCode(response.code, messageForHttpCode(response.code)),
            requestTime = GMTDate(),
            headers = buildHeaders(response),
            version = HttpProtocolVersion.HTTP_2_0,
            body = ByteReadChannel(response.body),
            callContext = Job() + dispatcher
        )

    private fun buildHeaders(response: ResponseDescriptor) = headersOf(*buildHeaderList(response).toTypedArray())

    private fun buildHeaderList(response: ResponseDescriptor) = response.headers
        .groupBy { it.name }
        .mapValues { it.value.mapNotNull { header -> header.value } }
        .entries
        .map { it.key to it.value } +
        (HttpHeaders.ContentType to listOf(response.mediaType))

    private fun mapRequest(request: HttpRequestData): HttpRequest = HttpRequest(
        method = request.method.value,
        scheme = request.url.protocol.name,
        host = request.url.host,
        port = request.url.port,
        path = request.url.encodedPath,
        params = request.url.parameters.toMap().mapValues { it.value[0] },
        headers = request.headers.toModel(),
        body = request.readBody()
    )

    private fun HttpRequestData.readBody() = when (val content = body) {
        is TextContent -> content.text
        is EmptyContent -> null
        else -> body.toString()
    }

    private fun Headers.toModel() = entries()
        .flatMap { (key, values) ->
            values.map { value -> Header(key, value) }
        }

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        logger.info("Intercepted request $data: Interceptor is ${internalConf.status}")
        return respondToRequest(data)
    }

    @InternalAPI
    private suspend fun respondToRequest(request: HttpRequestData): HttpResponseData = when (internalConf.status) {
        Mode.DISABLED -> config.delegate.execute(request)
        Mode.ENABLED -> responder.mockResponse(request)
        Mode.MIXED -> responder.mockResponseOrNull(request) ?: config.delegate.execute(request)
        Mode.RECORD -> writer?.recordCall(request) ?: error(RECORD_NOT_SUPPORTED_ERROR)
    }

    @InternalAPI
    @SuppressWarnings("TooGenericExceptionCaught")
    suspend fun RequestWriter.recordCall(request: HttpRequestData): HttpResponseData {
        var response: HttpResponseData? = null
        val record = try {
            response = config.delegate.execute(request)
            convertCallResult(request, response)
        } catch (e: Throwable) {
            RequestWriter.CallRecord(mapRequest(request), error = e)
        }

        saveFiles(record)
        return proceedWithCallResult(record, response)
    }

    private suspend fun convertCallResult(
        request: HttpRequestData,
        response: HttpResponseData
    ): RequestWriter.CallRecord {
        val body = response.readBody()
        return RequestWriter.CallRecord(
            mapRequest(request),
            response.toDescriptor(request.url),
            body?.toByteArray(),
            response.getMediaType()
        )
    }

    private suspend fun HttpResponseData.readBody(): String? = when (val content = body) {
        is ByteReadChannel -> content.readUTF8Line(Int.MAX_VALUE)
        else -> content?.toString()
    }

    private fun HttpResponseData.getMediaType(): MediaType? {
        val contentType = headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
        return contentType?.run { MediaType(contentType.contentType, contentType.contentSubtype) }
    }

    private fun proceedWithCallResult(record: RequestWriter.CallRecord, response: HttpResponseData?): HttpResponseData =
        record.error?.let {
            throw it
        } ?: response?.copyResponse(record.body) ?: error("Response is null")

    private fun HttpResponseData.copyResponse(body: ByteArray?) = HttpResponseData(
        statusCode = statusCode,
        requestTime = requestTime,
        headers = headers,
        version = version,
        body = body?.let { ByteReadChannel(it, 0, it.size) } ?: ByteReadChannel.Empty,
        callContext = callContext
    )

    internal fun HttpResponseData.toDescriptor(url: Url) = ResponseDescriptor(
        code = statusCode.value,
        bodyFile = url.toBodyFile() + "_body_",
        headers = headers.toModel()
    )

    private fun Url.toBodyFile(): String {
        val lastSegment = encodedPath.substringAfterLast('/')
        return lastSegment.takeUnless { it.isBlank() } ?: "index"
    }
}
