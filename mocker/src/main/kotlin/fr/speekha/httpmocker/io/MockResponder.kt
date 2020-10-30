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

class MockResponder<Request, Response>(
    private var providers: List<ScenarioProvider>,
    private var delay: Long,
    private val requestConverter: (Request) -> HttpRequest,
    private val responseBuilder: (Request, ResponseDescriptor) -> Response
) {

    private val logger = getLogger()

    fun mockResponse(request: Request): Response =
        mockResponseOrNull(request) ?: responseBuilder(request, responseNotFound())

    fun mockResponseOrNull(request: Request): Response? = providers.asSequence()
        .mapNotNull { provider ->
            logger.info("Looking up mock scenario for $request in $provider")
            provider.loadResponse(requestConverter(request))?.let { response ->
                executeMockResponse(response, request)
            }
        }
        .firstOrNull()

    private fun executeMockResponse(
        response: ResponseDescriptor,
        request: Request
    ): Response {
        logger.info("Response was found: $response")
        simulateDelay(response)
        return responseBuilder(request, response)
    }

    private fun simulateDelay(response: ResponseDescriptor) {
        when {
            response.delay > 0 -> Thread.sleep(response.delay)
            delay > 0 -> Thread.sleep(delay)
        }
    }
}
