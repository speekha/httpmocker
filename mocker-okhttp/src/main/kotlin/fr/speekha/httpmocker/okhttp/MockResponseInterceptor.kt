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

package fr.speekha.httpmocker.okhttp

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.NO_RECORDER_ERROR
import fr.speekha.httpmocker.RECORD_NOT_SUPPORTED_ERROR
import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.io.MockResponder
import fr.speekha.httpmocker.io.RequestWriter
import fr.speekha.httpmocker.okhttp.io.Recorder
import fr.speekha.httpmocker.okhttp.io.ResponseBuilder
import fr.speekha.httpmocker.okhttp.io.execute
import fr.speekha.httpmocker.okhttp.io.toGenericModel
import fr.speekha.httpmocker.scenario.ScenarioProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * A OkHTTP interceptor that can let requests through or block them and answer them with predefined responses.
 * Genuine network connections can also be recorded to create reusable offline scenarios.
 */
class MockResponseInterceptor
internal constructor(
    providers: List<ScenarioProvider>,
    requestWriter: RequestWriter?,
    delay: Long = 0
) : Interceptor {

    private val forbidRecord = requestWriter == null

    /**
     * Enables to set the interception mode. @see fr.speekha.httpmocker.okhttp.MockResponseInterceptor.Mode
     */
    var mode: Mode = Mode.DISABLED
        set(value) {
            if (value == Mode.RECORD && forbidRecord) {
                error(NO_RECORDER_ERROR)
            } else {
                field = value
            }
        }

    private val logger = getLogger()

    private val responder = MockResponder<Request, Response>(
        providers,
        delay,
        { it.toGenericModel() },
        { req, resp -> ResponseBuilder(req, resp).buildResponse() }
    )

    private val recorder = requestWriter?.let { Recorder(it) }

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()
        logger.info("Intercepted request $request: Interceptor is $mode")
        respondToRequest(chain, request)
    }

    private suspend fun respondToRequest(chain: Interceptor.Chain, request: Request) = when (mode) {
        Mode.DISABLED -> chain.execute()
        Mode.ENABLED -> responder.mockResponse(request)
        Mode.MIXED -> responder.mockResponseOrNull(request) ?: chain.execute()
        Mode.RECORD -> recorder?.recordCall(chain) ?: error(RECORD_NOT_SUPPORTED_ERROR)
    }
}
