/*
 *  Copyright 2019-2021 David Blanc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package fr.speekha.httpmocker.io

import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.scenario.ScenarioProvider
import org.junit.Test
import kotlin.test.assertEquals

class MockResponderTest {

    @Test
    fun `should run responder`() = runTest {
        val scenario = object : ScenarioProvider {
            override fun loadResponse(request: HttpRequest): ResponseDescriptor =
                ResponseDescriptor(
                    body = "Response"
                )
        }
        val responder = MockResponder(
            listOf(scenario),
            0,
            this@MockResponderTest::createRequest,
            this@MockResponderTest::createResponse
        )
        val response = responder.mockResponse(Request("path"))
        assertEquals("Response", response.body)
    }

    private data class Request(val path: String)

    private fun createRequest(request: Request) = HttpRequest(path = request.path)

    private data class Response(
        val request: Request,
        val body: String
        )

    private fun createResponse(request: Request, response: ResponseDescriptor) =
        Response(request, response.body)

}