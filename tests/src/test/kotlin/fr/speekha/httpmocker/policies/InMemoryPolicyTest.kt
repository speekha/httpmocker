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

package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.MockResponseInterceptor.Mode.ENABLED
import fr.speekha.httpmocker.buildRequest
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("In-Memory Policy")
class InMemoryPolicyTest {

    private val mapper = JacksonMapper()

    private val policy = InMemoryPolicy(mapper)

    @Nested
    @DisplayName("Given an in memory policy")
    inner class TestPolicy {

        private val interceptor = MockResponseInterceptor.Builder()
            .decodeScenarioPathWith(policy)
            .loadFileWith(policy::matchRequest)
            .parseScenariosWith(mapper)
            .setInterceptorStatus(ENABLED)
            .build()

        @Test
        @DisplayName("When processing a URL, then resulting path should be the input URL")
        fun `should return URL as path`() {
            val url = "http://www.test.fr/path?param=1"
            assertEquals(url, policy.getPath(buildRequest(url)))
        }

        @Test
        @DisplayName("When processing a request, then an existing match should be found")
        fun `should allow to retrieve a scenario based on a URL`() {
            val url = "http://www.test.fr/path1?param=1"
            policy.addMatcher(
                url, Matcher(
                    RequestDescriptor(method = "GET"),
                    ResponseDescriptor(
                        code = 200,
                        body = "get some body",
                        mediaType = "text/plain"
                    )
                )
            )

            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            val getResponse = client.newCall(buildRequest(url, method = "GET")).execute()

            assertEquals(200, getResponse.code())
            assertEquals("get some body", getResponse.body()?.string())
        }

        @Test
        @DisplayName("When processing a request with several possible matches, then all matching entries should be returned")
        fun `should allow to add several matchers for the same URL`() {
            val url = "http://www.test.fr/path1?param=1"
            policy.addMatcher(
                url, Matcher(
                    RequestDescriptor(method = "GET"),
                    ResponseDescriptor(
                        code = 200,
                        body = "get some body",
                        mediaType = "text/plain"
                    )
                )
            )
            policy.addMatcher(
                url, Matcher(
                    RequestDescriptor(method = "POST"),
                    ResponseDescriptor(
                        code = 200,
                        body = "post some body",
                        mediaType = "text/plain"
                    )
                )
            )

            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            val getResponse = client.newCall(buildRequest(url, listOf(), "GET")).execute()
            val postResponse = client.newCall(buildRequest(url, listOf(), "POST", "body")).execute()

            assertEquals("get some body", getResponse.body()?.string())
            assertEquals("post some body", postResponse.body()?.string())
        }

        @Test
        @DisplayName("When processing requests with different URLs, then corresponding match should be returned")
        fun `should allow to add matchers for different URLs`() {
            val url1 = "http://www.test.fr/path1?param=1"
            val url2 = "http://www.test.fr/path2?param=1"
            policy.addMatcher(
                url1, Matcher(
                    RequestDescriptor(method = "GET"),
                    ResponseDescriptor(
                        code = 200,
                        body = "first body",
                        mediaType = "text/plain"
                    )
                )
            )
            policy.addMatcher(
                url2, Matcher(
                    RequestDescriptor(method = "GET"),
                    ResponseDescriptor(
                        code = 200,
                        body = "second body",
                        mediaType = "text/plain"
                    )
                )
            )

            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            val response1 = client.newCall(buildRequest(url1, listOf(), "GET")).execute()
            val response2 = client.newCall(buildRequest(url2, listOf(), "GET")).execute()

            assertEquals("first body", response1.body()?.string())
            assertEquals("second body", response2.body()?.string())
        }
    }
}
