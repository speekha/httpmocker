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

package fr.speekha.httpmocker.client.okhttp

import fr.speekha.httpmocker.HTTP_METHOD_GET
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.client.HttpClientTester
import fr.speekha.httpmocker.client.StaticMockTests
import fr.speekha.httpmocker.client.TestWithServer
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.okhttp.builder.mockInterceptor
import fr.speekha.httpmocker.serialization.Mapper
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Response
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("UNUSED_PARAMETER")
@DisplayName("Static Mocks with OkHttp")
class StaticMockTests :
    StaticMockTests<Response, OkHttpClient>(),
    HttpClientTester<Response, OkHttpClient> by OkHttpTests() {

    @Nested
    @DisplayName("Given a mock interceptor in mixed mode")
    inner class StackedInterceptors {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When several interceptors are stacked, " +
                "then each should delegate to the next one requests it can't answer"
        )
        fun `should allow to stack several interceptors thanks to mixed mode`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            initFilingPolicy(type)

            enqueueServerResponse(TestWithServer.REQUEST_OK_CODE, "server response")

            val inMemoryInterceptor =
                mockInterceptor {
                    useDynamicMocks { request ->
                        ResponseDescriptor(
                            code = TestWithServer.REQUEST_OK_CODE,
                            body = "in memory response",
                            mediaType = "text/plain"
                        ).takeIf {
                            request.path == "/inMemory" && request.method == HTTP_METHOD_GET
                        }
                    }
                    setInterceptorStatus(Mode.MIXED)
                }

            val fileBasedInterceptor =
                mockInterceptor {
                    decodeScenarioPathWith(filingPolicy)
                    loadFileWith(loadingLambda)
                    parseScenariosWith(mapper)
                    setInterceptorStatus(Mode.MIXED)
                }

            client = OkHttpClient.Builder()
                .addInterceptor(inMemoryInterceptor)
                .addInterceptor(fileBasedInterceptor)
                .build()

            Assertions.assertEquals("in memory response", executeRequest("inMemory").body?.string())
            Assertions.assertEquals("file response", executeRequest("fileMatch").body?.string())
            Assertions.assertEquals("server response", executeRequest("serverMatch").body?.string())
        }
    }
}
