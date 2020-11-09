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
import fr.speekha.httpmocker.io.MockResponder
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.util.InternalAPI
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
        ::mapRequest,
        ::mapResponse
    )

    private val recorder: Recorder? = config.configBuilder.buildRecorder()?.let { Recorder(it, config.delegate) }

    private val forbidRecord: Boolean
        get() = recorder == null

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
        Mode.RECORD -> recorder?.executeAndRecordCall(request) ?: error(RECORD_NOT_SUPPORTED_ERROR)
    }
}
