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

package fr.speekha.httpmocker.io

import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.responseNotFound
import fr.speekha.httpmocker.scenario.ScenarioProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull

class MockResponder<Request, Response>(
    private var providers: List<ScenarioProvider>,
    private var delay: Long,
    private val requestConverter: (Request) -> HttpRequest,
    private val responseBuilder: (Request, ResponseDescriptor) -> Response
) {

    private val logger = getLogger()

    suspend fun mockResponse(request: Request): Response =
        mockResponseOrNull(request) ?: responseBuilder(request, responseNotFound())

    suspend fun mockResponseOrNull(request: Request): Response? = providers.asFlow()
        .mapNotNull { provider ->
            logger.info("Looking up mock scenario for $request in $provider")
            provider.loadResponse(requestConverter(request))?.let { response ->
                executeMockResponse(response, request)
            }
        }
        .firstOrNull()

    private suspend fun executeMockResponse(
        response: ResponseDescriptor,
        request: Request
    ): Response {
        logger.info("Response was found: $response")
        simulateDelay(response)
        return responseBuilder(request, response)
    }

    private suspend fun simulateDelay(response: ResponseDescriptor) {
        when {
            response.delay > 0 -> delay(response.delay)
            delay > 0 -> delay(delay)
        }
    }
}
