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

import fr.speekha.httpmocker.RequestRecorder.CallRecord
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.scenario.ScenarioProvider
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * A OkHTTP interceptor that can let requests through or block them and answer them with predefined responses.
 * Genuine network connections can also be recorded to create reusable offline scenarios.
 */
class MockResponseInterceptor
internal constructor(
    private var providers: List<ScenarioProvider>,
    private var requestRecorder: RequestRecorder?
) : Interceptor {

    /**
     * An arbitrary delay to include when answering requests in order to have a realistic behavior (GUI can display
     * loaders, etc.)
     */
    internal var delay: Long = 0

    /**
     * Enables to set the interception mode. @see fr.speekha.httpmocker.MockResponseInterceptor.Mode
     */
    var mode: Mode = Mode.DISABLED
        set(value) {
            if (value == Mode.RECORD && requestRecorder == null) {
                error(NO_RECORDER_ERROR)
            } else {
                field = value
            }
        }

    private val logger = getLogger()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logger.info("Intercepted request $request: Interceptor is $mode")
        return respondToRequest(chain, request)
    }

    private fun respondToRequest(chain: Interceptor.Chain, request: Request) = when (mode) {
        Mode.DISABLED -> executeNetworkCall(chain)
        Mode.ENABLED -> mockResponse(request) ?: buildResponse(request, responseNotFound(), null)
        Mode.MIXED -> mockResponse(request) ?: executeNetworkCall(chain)
        Mode.RECORD -> recordCall(chain)
    }

    private fun executeNetworkCall(chain: Interceptor.Chain) = chain.proceed(chain.request())

    private fun mockResponse(request: Request): Response? = providers.asSequence()
        .mapNotNull { provider ->
            logger.info("Looking up mock scenario for $request in $provider")
            provider.loadResponse(request)?.let { response ->
                executeMockResponse(response, request, provider)
            }
        }
        .firstOrNull()

    private fun executeMockResponse(
        response: ResponseDescriptor,
        request: Request,
        provider: ScenarioProvider
    ): Response {
        logger.info("Response was found: $response")
        simulateDelay(response)
        return buildResponse(request, response, provider)
    }

    private fun simulateDelay(response: ResponseDescriptor) {
        when {
            response.delay > 0 -> Thread.sleep(response.delay)
            delay > 0 -> Thread.sleep(delay)
        }
    }

    private fun buildResponse(
        request: Request,
        response: ResponseDescriptor,
        provider: ScenarioProvider?
    ): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(response.code)
        .message(messageForHttpCode(response.code))
        .addHeaders(response)
        .body(loadResponseBody(request, response, provider))
        .build()

    private fun Response.Builder.addHeaders(response: ResponseDescriptor) = apply {
        header("Content-type", response.mediaType)
        response.headers.forEach {
            if (it.value != null) {
                header(it.name, it.value)
            }
        }
    }

    private fun loadResponseBody(
        request: Request,
        response: ResponseDescriptor,
        provider: ScenarioProvider?
    ) = ResponseBody.create(
        MediaType.parse(response.mediaType), response.bodyFile?.let {
            logger.info("Loading response body from file: $it")
            provider?.loadResponseBody(request, it)
        } ?: response.body.toByteArray()
    )

    private fun responseNotFound(body: String = "Page not found") =
        ResponseDescriptor(code = 404, body = body)

    private fun recordCall(chain: Interceptor.Chain): Response = requestRecorder?.let { recorder ->
        val record = convertCallResult(chain)
        recorder.saveFiles(record)
        proceedWithCallResult(record)
    } ?: error(RECORD_NOT_SUPPORTED_ERROR)

    @SuppressWarnings("TooGenericExceptionCaught")
    private fun convertCallResult(chain: Interceptor.Chain): CallRecord = try {
        val response = executeNetworkCall(chain)
        val body = response.body()?.bytes()
        val contentType = response.body()?.contentType()
        CallRecord(chain.request(), response, body, contentType)
    } catch (e: Throwable) {
        CallRecord(chain.request(), error = e)
    }

    private fun proceedWithCallResult(record: CallRecord): Response? = if (record.error != null) {
        throw record.error
    } else {
        record.response?.copyResponse(record.body)
    }

    private fun messageForHttpCode(httpCode: Int) =
        HTTP_RESPONSES_CODE[httpCode] ?: "Unknown error code"
}
