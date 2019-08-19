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

package fr.speekha.httpmocker.interceptor

import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.MockResponseInterceptor.Mode.DISABLED
import fr.speekha.httpmocker.MockResponseInterceptor.Mode.ENABLED
import fr.speekha.httpmocker.buildRequest
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.scenario.RequestCallback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.StringStartsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Dynamic Mocks")
class DynamicMockTests : TestWithServer() {

    @Nested
    @DisplayName("Given an mock interceptor that is disabled")
    inner class DisabledInterceptor {

        @Test
        @DisplayName("When a request is made, then the interceptor should not interfere with it")
        fun `should not interfere with requests when disabled`() {
            setupProvider(DISABLED) { null }
            enqueueServerResponse(200, "body")

            val response = executeGetRequest("")

            assertResponseCode(response, 200, "OK")
            assertEquals("body", response.body()?.string())
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor with a dynamic callback")
    inner class DynamicTests {

        @Test
        @DisplayName("When no response is provided, then a 404 error should occur")
        fun `should return a 404 error when response is not found`() {
            setupProvider(ENABLED) { null }

            val response = executeGetRequest("/unknown")

            assertResponseCode(response, 404, "Not Found")
        }

        @Test
        @DisplayName("When an error occurs while answering a request, then a 404 error should occur")
        fun `should return a 404 error when an exception occurs`() {
            setupProvider(ENABLED) { error("Unexpected error") }

            val response = executeGetRequest("/unknown")

            assertResponseCode(response, 404, "Not Found")
        }

        @Test
        @DisplayName("When an error occurs while answering a request, then the request body should be the error")
        fun `should return the error message when an exception occurs`() {
            setupProvider(ENABLED) { error("Unexpected error") }

            val response = executeGetRequest("/unknown").body()?.string()

            assertThat(
                response, StringStartsWith(
                    "java.lang.IllegalStateException: Unexpected error\n" +
                            "\tat fr.speekha.httpmocker.interceptor.DynamicMockTests"
                )
            )
        }

        @Test
        @DisplayName("When a lambda is provided, then it should be used to answer requests")
        fun `should reply with a dynamically generated response`() {
            setupProvider {
                ResponseDescriptor(code = 202, body = "some random body")
            }
            val response = client.newCall(
                buildRequest(
                    url,
                    method = "GET"
                )
            ).execute()

            assertEquals(202, response.code())
            assertEquals("some random body", response.body()?.string())
        }

        @Test
        @DisplayName("When a stateful callback is provided, then it should be used to answer requests")
        fun `should reply with a stateful callback`() {
            val body = "Time: ${System.currentTimeMillis()}"
            val callback = object : RequestCallback {
                override fun loadResponse(request: Request) =
                    ResponseDescriptor(code = 202, body = body)
            }
            setupProvider(callback)

            val response = client.newCall(
                buildRequest(
                    url,
                    method = "GET"
                )
            ).execute()

            assertEquals(202, response.code())
            assertEquals(body, response.body()?.string())
        }

        @Test
        @DisplayName("When several callbacks are provided, then they should be called in turn to find the appropriate response")
        fun `should support multiple callbacks`() {
            val result1 = "First mock"
            val result2 = "Second mock"

            interceptor = MockResponseInterceptor.Builder()
                .useDynamicMocks {
                    if (it.url().toString().contains("1"))
                        ResponseDescriptor(body = result1)
                    else null
                }.useDynamicMocks {
                    ResponseDescriptor(body = result2)
                }
                .setInterceptorStatus(ENABLED)
                .build()

            client = OkHttpClient.Builder().addInterceptor(interceptor).build()

            val response1 =
                client.newCall(
                    buildRequest(
                        "http://www.test.fr/request1",
                        method = "GET"
                    )
                ).execute()
            val response2 =
                client.newCall(
                    buildRequest(
                        "http://www.test.fr/request2",
                        method = "GET"
                    )
                ).execute()

            assertEquals(result1, response1.body()?.string())
            assertEquals(result2, response2.body()?.string())
        }

        private fun setupProvider(callback: RequestCallback) {
            interceptor = MockResponseInterceptor.Builder()
                .useDynamicMocks(callback)
                .setInterceptorStatus(ENABLED)
                .build()

            client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        }
    }

    private fun setupProvider(
        status: MockResponseInterceptor.Mode = ENABLED,
        callback: (Request) -> ResponseDescriptor?
    ) {
        interceptor = MockResponseInterceptor.Builder()
            .useDynamicMocks(callback)
            .setInterceptorStatus(status)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    companion object {
        const val url = "http://www.test.fr/path1?param=1"
    }
}
