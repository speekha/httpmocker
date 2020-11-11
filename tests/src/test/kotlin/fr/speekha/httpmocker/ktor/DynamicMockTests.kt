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

package fr.speekha.httpmocker.ktor

import fr.speekha.httpmocker.HttpClientTester
import fr.speekha.httpmocker.Mode.DISABLED
import fr.speekha.httpmocker.TestWithServer.Companion.REQUEST_OK_CODE
import fr.speekha.httpmocker.assertThrows
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Dynamic Mocks with Ktor")
class DynamicMockTests : HttpClientTester<HttpResponse> by KtorTests() {

    @Nested
    @DisplayName("Given an mock interceptor that is disabled")
    inner class DisabledInterceptor {

        @Test
        @DisplayName("When a request is made, then the interceptor should not interfere with it")
        fun `should not interfere with requests when disabled`() = runBlocking {
            setupProvider({ null }, status = DISABLED)
            enqueueServerResponseTmp(REQUEST_OK_CODE, "body")

            val response: String = executeRequest("/").readText()

            assertEquals("body", response)
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor with a dynamic callback")
    inner class DynamicTests {

        @Test
        @DisplayName("When no response is provided, then a 404 error should occur")
        fun `should return a 404 error when response is not found`() = runBlocking {
            setupProvider({ null })

            val response: HttpResponse = executeRequest("/unknown")

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        @DisplayName("When an error occurs while answering a request, then the exception should be let through")
        fun `should let exceptions through when they occur`() {
            runBlocking {
                setupProvider({ error("Unexpected error") })

                assertThrows<IllegalStateException>("Unexpected error") {
                    executeRequest("/unknown")
                }
            }
        }

        @Test
        @DisplayName("When a lambda is provided, then it should be used to answer requests")
        fun `should reply with a dynamically generated response`() = runBlocking {
            val resultCode = HttpStatusCode.Accepted
            val body = "some random body"
            setupProvider({
                ResponseDescriptor(code = resultCode.value, body = body)
            })
            val response = executeRequest(url)

            assertEquals(resultCode, response.status)
            assertEquals(body, response.readText())
            Unit
        }

        @Test
        @DisplayName("When a stateful callback is provided, then it should be used to answer requests")
        fun `should reply with a stateful callback`() = runBlocking {
            val resultCode = 201
            val body = "Time: ${System.currentTimeMillis()}"
            setupProvider({ ResponseDescriptor(code = resultCode, body = body) })

            val response = executeRequest(url)

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(body, response.readText())
            Unit
        }

        @Test
        @DisplayName(
            "When several callbacks are provided, " +
                "then they should be called in turn to find the appropriate response"
        )
        fun `should support multiple callbacks`() = runBlocking {
            val result1 = "First mock"
            val result2 = "Second mock"

            setupProvider(
                { request ->
                    ResponseDescriptor(body = result1).takeIf {
                        request.path.contains("1")
                    }
                }, {
                    ResponseDescriptor(body = result2)
                }
            )

            val response1 = executeRequest("http://www.test.fr/request1")
            val response2 = executeRequest("http://www.test.fr/request2")

            assertEquals(result1, response1.readText())
            assertEquals(result2, response2.readText())
            Unit
        }

        @Test
        @DisplayName(
            "When the response is an error, then the proper exception should be thrown"
        )
        fun `should support exception results`() {
            runBlocking {

                setupProvider({
                    error("Should throw an error")
                })

                assertThrows<IllegalStateException> {
                    executeRequest("http://www.test.fr/request1")
                }
            }
        }

        @Test
        @DisplayName(
            "When 2 request are executed simultaneously then proper responses are returned"
        )
        fun `should synchronize loadResponse`() {
            setupProvider({
                ResponseDescriptor(body = "body${it.path.last()}")
            })
            repeat(1000) {
                testSimultaneousRequests()
            }
        }

        private fun testSimultaneousRequests() {
            val response: Array<HttpResponse?> = arrayOfNulls(2)
            val running = MutableStateFlow(false)
            runBlocking {
                response.indices.forEach { i ->
                    launch(Dispatchers.IO) {
                        response[i] = delayedRequest(running, i)
                    }
                }
                running.emit(true)
            }

            runBlocking {
                response.indices.forEach { i ->
                    assertEquals("body$i", (response[i] ?: fail("Response is null")).readText())
                }
            }
        }

        private suspend fun delayedRequest(lock: StateFlow<Boolean>, i: Int): HttpResponse {
            lock.first { it }
            return executeRequest("/request$i")
        }
    }
}
