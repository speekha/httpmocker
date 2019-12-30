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

package fr.speekha.httpmocker.io

import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.scenario.ScenarioProvider
import okhttp3.Request
import okhttp3.Response

internal class MockResponder(
    private var providers: List<ScenarioProvider>,
    private var delay: Long
) {

    private val logger = getLogger()

    fun mockResponse(request: Request): Response = mockResponseOrNull(request) ?: ResponseBuilder(
        request
    ).buildResponse()

    fun mockResponseOrNull(request: Request): Response? = providers.asSequence()
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
        return ResponseBuilder(request, response, provider).buildResponse()
    }

    private fun simulateDelay(response: ResponseDescriptor) {
        when {
            response.delay > 0 -> Thread.sleep(response.delay)
            delay > 0 -> Thread.sleep(delay)
        }
    }
}
