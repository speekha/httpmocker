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

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.Mode.ENABLED
import fr.speekha.httpmocker.Mode.MIXED
import fr.speekha.httpmocker.buildRequest
import fr.speekha.httpmocker.builder.mockInterceptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.SingleFilePolicy
import fr.speekha.httpmocker.serialization.Mapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.MatcherAssert
import org.hamcrest.core.StringStartsWith
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.system.measureTimeMillis

@Suppress("UNUSED_PARAMETER")
class StaticMockTests : TestWithServer() {

    private val loadingLambda: (String) -> InputStream? = mock {
        on { invoke(any()) } doAnswer { javaClass.classLoader.getResourceAsStream(it.getArgument(0)) }
    }

    private lateinit var filingPolicy: FilingPolicy

    @Nested
    @DisplayName("Given an enabled mock interceptor with static scenarios")
    inner class StaticTests {
        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When a request is answered with a scenario, " +
                    "then its path should be computed by the file policy"
        )
        fun `should delegate path resolutions`(title: String, mapper: Mapper, type: String) {
            setUpInterceptor(ENABLED, mapper, type)

            val request = initRequest("/request")
            client.newCall(request).execute()

            verify(filingPolicy).getPath(request)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When no scenario file is provided, " +
                    "then a 404 error should occur"
        )
        fun `should return a 404 error when response is not found`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/unknown")

            assertResponseCode(response, NOT_FOUND_CODE, NOT_FOUND_MESSAGE)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When no requests in the file matches, " +
                    "then a 404 error should occur"
        )
        fun `should return a 404 error when no request matches the criteria`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            whenever(loadingLambda.invoke(any())) doAnswer {
                loadingError()
            }
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/no_match")

            assertResponseCode(response, NOT_FOUND_CODE, NOT_FOUND_MESSAGE)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When an error occurs while loading scenarios, " +
                    "then a 404 error should occur"
        )
        fun `should return a 404 error when an exception occurs`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            whenever(loadingLambda.invoke(any())) doAnswer {
                loadingError()
            }
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/unknown")

            assertResponseCode(response, NOT_FOUND_CODE, NOT_FOUND_MESSAGE)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When loaded scenario has neither response nor error, " +
                    "then a 404 error should occur"
        )
        fun `should return a 404 error when the matching request has no response or error`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/no_response_no_error")

            assertResponseCode(response, NOT_FOUND_CODE, NOT_FOUND_MESSAGE)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When an error is configured as an answer, " +
                    "then the corresponding exception should be thrown"
        )
        fun `should throw a mocked exception`(title: String, mapper: Mapper, type: String) {
            setUpInterceptor(ENABLED, mapper, type)

            val exception = assertThrows<IOException> {
                executeGetRequest("/exception")
            }
            assertNull(exception.message)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When an error with a message is configured as an answer, " +
                    "then the corresponding exception with message should be thrown"
        )
        fun `should throw a mocked exception with message`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val exception = assertThrows<IOException> {
                executeGetRequest("/exception_with_message")
            }
            assertEquals("An exception message", exception.message)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When an error occurs while answering a request, " +
                    "then the request body should be the error"
        )
        fun `should return the error message when an exception occurs`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            whenever(loadingLambda.invoke(any())) doAnswer {
                loadingError()
            }
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/unknown").body?.string()

            MatcherAssert.assertThat(
                response, StringStartsWith(
                    "java.lang.IllegalStateException: Loading error\n" +
                            "\tat fr.speekha.httpmocker.interceptor.StaticMockTests"
                )
            )
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a response is found, then default HTTP code should be 200")
        fun `should return a 200 when response is found`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/request")

            assertResponseCode(response, REQUEST_OK_CODE, REQUEST_OK_MESSAGE)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When a response is found and response body is in JSON scenario, " +
                    "then it should be loaded from scenario"
        )
        fun `should return a predefined response body from json descriptor`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/request")

            assertEquals("simple body", response.body?.string())
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor with static and dynamic mocks")
    inner class StaticAndDynamic {
        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When a request is answered, " +
                    "then dynamic mocks should be tried first before static ones"
        )
        fun `should support dynamic and static mocks together`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            val result1 = "Dynamic"
            val result2 = "simple body"

            initFilingPolicy(type)

            interceptor = mockInterceptor {
                useDynamicMocks { request ->
                    ResponseDescriptor(body = result1).takeIf {
                        request.url.toString().contains("dynamic")
                    }
                }
                decodeScenarioPathWith(filingPolicy)
                loadFileWith(loadingLambda)
                parseScenariosWith(mapper)
                setInterceptorStatus(ENABLED)
            }

            client = OkHttpClient.Builder().addInterceptor(interceptor).build()

            val response1 =
                client.newCall(buildRequest("http://www.test.fr/dynamic", method = "GET"))
                    .execute()
            val response2 =
                client.newCall(buildRequest("http://www.test.fr/request", method = "GET"))
                    .execute()

            assertEquals(result1, response1.body?.string())
            assertEquals(result2, response2.body?.string())
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor with response bodies in separate files")
    inner class SeparateFile {
        @ParameterizedTest(name = "Mapper: {0}")
        @DisplayName(
            "When a response is found, " +
                    "then the body should be loaded from the file next to it"
        )
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        fun `should return a predefined response body from separate file`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/body_file")

            assertEquals("separate body file", response.body?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When scenario file path is not empty, " +
                    "then response body should be in the same folder by default"
        )
        fun `should return a predefined response body from separate file in the same folder`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/folder/request_in_folder")

            assertResponseCode(response, REQUEST_OK_CODE, REQUEST_OK_MESSAGE)
            assertEquals("separate body file", response.body?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When response body is in a child folder, " +
                    "then response body path should be read from that folder"
        )
        fun `should return a predefined response body from separate file in a different folder`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/request_in_other_folder")

            assertResponseCode(response, REQUEST_OK_CODE, REQUEST_OK_MESSAGE)
            assertEquals("separate body file", response.body?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When response body is in a parent folder, " +
                    "then response body path should be read from that folder"
        )
        fun `should return a predefined response body from separate file in a parent folder`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/folder2/request_in_other_folder")

            assertResponseCode(response, REQUEST_OK_CODE, REQUEST_OK_MESSAGE)
            assertEquals("separate body file", response.body?.string())
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor and a response scenario")
    inner class ResponseBuilding {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When a request is answered, then the proper " +
                    "headers should be set in the response"
        )
        fun `should return proper headers`(title: String, mapper: Mapper, type: String) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/request")

            assertResponseCode(response, REQUEST_OK_CODE, REQUEST_OK_MESSAGE)
            assertEquals("simple header", response.header("testHeader"))
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When the response is a redirect, " +
                    "then the response should have a HTTP code 302 and a location"
        )
        fun `should handle redirects`(title: String, mapper: Mapper, type: String) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/redirect")

            assertResponseCode(response, 302, "Found")
            assertEquals("http://www.google.com", response.header("Location"))
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When a request is answered, " +
                    "then the proper content type should be set in the response"
        )
        fun `should handle media type`(title: String, mapper: Mapper, type: String) {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeGetRequest("/mediatype")

            assertResponseCode(response, REQUEST_OK_CODE, REQUEST_OK_MESSAGE)
            assertEquals("application", response.body?.contentType()?.type)
            assertEquals("application/json", response.header("Content-type"))
            assertEquals("json", response.body?.contentType()?.subtype)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When a request with no specific delay is answered, " +
                    "then default delay should be used"
        )
        fun `should allow to delay all responses`(title: String, mapper: Mapper, type: String) {
            setUpInterceptor(ENABLED, mapper, type, 50)

            val delay = measureTimeMillis {
                executeGetRequest("/request").body?.string()
            }

            val threshold = 50
            assertTrue(delay >= threshold, "Time was $delay (< $threshold ms)")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When a request with a specific delay is answered, " +
                    "then that delay should be used"
        )
        fun `should allow to delay responses based on configuration`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val delay = measureTimeMillis {
                executeGetRequest("/delay").body?.string()
            }

            val noDelay = measureTimeMillis {
                executeGetRequest("/request").body?.string()
            }

            val threshold = 50
            assertTrue(delay >= threshold, "Time was $delay (< $threshold ms)")
            assertTrue(noDelay < threshold, "Time without delay was $noDelay (> $threshold ms)")
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor and no filing policy")
    inner class DefaultPolicy {

        private fun setupInterceptor(mapper: Mapper, type: String) {
            interceptor = mockInterceptor {
                loadFileWith(loadingLambda)
                parseScenariosWith(mapper)
                setInterceptorStatus(ENABLED)
            }

            client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should use a MirrorPathPolicy as default")
        fun `should use mirror path as default file policy`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setupInterceptor(mapper, type)

            val get = executeGetRequest("/request").body?.string()

            assertEquals("simple body", get)
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor and a single file policy")
    inner class UrlMatching {

        fun setupInterceptor(scenarioFile: String, mapper: Mapper, type: String) {
            interceptor = mockInterceptor {
                decodeScenarioPathWith(SingleFilePolicy("$scenarioFile.$type"))
                loadFileWith(loadingLambda)
                parseScenariosWith(mapper)
                setInterceptorStatus(ENABLED)
            }

            client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match URL protocol")
        fun `should take http protocol into account`(title: String, mapper: Mapper, type: String) {
            setupInterceptor("protocol", mapper, type)

            val get = executeGetRequest("/protocol").body?.string()

            assertEquals("HTTP", get)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match the URL host")
        fun `should select response based on host`(title: String, mapper: Mapper, type: String) {
            setupInterceptor(SINGLE_FILE, mapper, type)

            val request = buildRequest("http://hostTest.com:12345/anyUrl")

            assertEquals("based on host", client.newCall(request).execute().body?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match the URL port")
        fun `should select response based on port`(title: String, mapper: Mapper, type: String) {
            setupInterceptor(SINGLE_FILE, mapper, type)

            val request = buildRequest("http://someHost.com:45612/anyUrl")

            assertEquals("based on port", client.newCall(request).execute().body?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match the URL path")
        fun `should select response based on URL path`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setupInterceptor(SINGLE_FILE, mapper, type)

            val request = buildRequest("http://someHost.com:12345/aTestUrl")

            assertEquals("based on URL", client.newCall(request).execute().body?.string())
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor with multiple file policies")
    inner class MultiplePolicies {

        private lateinit var policy1: FilingPolicy
        private lateinit var policy2: FilingPolicy

        private fun setupInterceptor(mapper: Mapper, type: String) {
            policy1 = mock {
                on { this.getPath(any()) } doReturn "incorrect"
            }
            policy2 = mock {
                on { this.getPath(any()) } doReturn "incorrect"
            }

            interceptor = mockInterceptor {
                decodeScenarioPathWith(policy1)
                decodeScenarioPathWith(policy2)
                loadFileWith(loadingLambda)
                parseScenariosWith(mapper)
                setInterceptorStatus(ENABLED)
            }

            client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When the first policy provides a match, then it should be used")
        fun `should use first policy if possible`(title: String, mapper: Mapper, type: String) {
            setupInterceptor(mapper, type)
            whenever(policy1.getPath(any())).thenReturn("request.$type")

            val get = executeGetRequest("/request").body?.string()
            assertEquals("simple body", get)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When the first policy does not provide a match, then the second one should be used")
        fun `should use second policy as fallback`(title: String, mapper: Mapper, type: String) {
            setupInterceptor(mapper, type)
            whenever(policy2.getPath(any())).thenReturn("request.$type")
            val get = executeGetRequest("/request").body?.string()
            assertEquals("simple body", get)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When none of the policies provide a match, then an 404 error should occur")
        fun `should handle all policy failure`(title: String, mapper: Mapper, type: String) {
            setupInterceptor(mapper, type)
            val get = executeGetRequest("/request")
            assertEquals(404, get.code)
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor and a scenario with multiple request patterns")
    inner class RequestMatching {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        fun `should take http method into account`(title: String, mapper: Mapper, type: String) {
            setUpInterceptor(ENABLED, mapper, type)

            val get = executeGetRequest("/method").body?.string()
            val post = executeRequest("/method", "POST", "").body?.string()
            val put = executeRequest("/method", "PUT", "").body?.string()
            val delete = executeRequest("/method", "DELETE", "").body?.string()

            assertEquals("get", get)
            assertEquals("post", post)
            assertEquals("put", put)
            assertEquals("delete", delete)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match present query parameters")
        fun `should select response based on query params`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val param1 = executeGetRequest("/query_param?param=1").body?.string()
            val param2 = executeGetRequest("/query_param?param=2").body?.string()

            assertEquals("param A", param1)
            assertEquals("param B", param2)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match absent query parameters")
        fun `should select response based on absent query params`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val param1 = executeGetRequest("/absent_query_param?param1=1").body?.string()
            val param2 = executeGetRequest("/absent_query_param?param1=1&param2=2")

            assertEquals("Body found", param1)
            assertEquals(NOT_FOUND_CODE, param2.code)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match present headers")
        fun `should select response based on headers`(title: String, mapper: Mapper, type: String) {
            setUpInterceptor(ENABLED, mapper, type)

            val noHeaders = executeGetRequest("/headers").body?.string()
            val headers = executeGetRequest(
                "/headers",
                listOf(
                    "header1" to "1",
                    "header1" to "2",
                    "header2" to "3"
                )
            ).body?.string()
            val header1 = executeGetRequest(
                "/headers",
                listOf("header1" to "1")
            ).body?.string()
            val header2 = executeGetRequest(
                "/headers",
                listOf("header2" to "2")
            ).body?.string()

            assertEquals("no header", noHeaders)
            assertEquals("with header 1", header1)
            assertEquals("with header 2", header2)
            assertEquals("with headers", headers)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match absent headers")
        fun `should select response based on absent headers`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val correctHeader =
                executeGetRequest("/absent_header", listOf("header1" to "1")).body?.string()
            val extraHeader =
                executeGetRequest("/absent_header", listOf("header1" to "1", "header2" to "2"))

            assertEquals("Body found", correctHeader)
            assertEquals(NOT_FOUND_CODE, extraHeader.code)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match request body")
        fun `should select response based on request body`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val match = executeRequest("/body_matching", "POST", "azer1zere").body?.string()
            val noMatch = executeRequest("/body_matching", "POST", "azerzere").body?.string()

            assertEquals("matched", match)
            assertEquals("no match", noMatch)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When a request with exact match is answered, " +
                    "then match should not allow extra headers or parameters"
        )
        fun `should select response based on exact matches`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            setUpInterceptor(ENABLED, mapper, type)

            val exactHeader = executeGetRequest("/exact_match", listOf("header1" to "1"))
            val extraHeader =
                executeGetRequest("/exact_match", listOf("header1" to "1", "header2" to "2"))
            val exactParam = executeGetRequest("/exact_match?param1=1")
            val extraParam = executeGetRequest("/exact_match?param1=1&param2=2")

            assertEquals("Exact headers", exactHeader.body?.string())
            assertEquals(NOT_FOUND_CODE, extraHeader.code)
            assertEquals("Exact params", exactParam.body?.string())
            assertEquals(NOT_FOUND_CODE, extraParam.code)
        }
    }

    @Nested
    @DisplayName("Given a mock interceptor in mixed mode")
    inner class MixedMode {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is not mocked, then it should go to the server")
        fun `should support mixed mode to execute request when no response is found locally`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            enqueueServerResponse(REQUEST_OK_CODE, "body")
            setUpInterceptor(MIXED, mapper, type)

            val serverResponse = executeGetRequest("")
            val localResponse = executeGetRequest("/request")

            assertResponseCode(serverResponse, REQUEST_OK_CODE, REQUEST_OK_MESSAGE)
            assertEquals("body", serverResponse.body?.string())
            assertResponseCode(localResponse, REQUEST_OK_CODE, REQUEST_OK_MESSAGE)
            assertEquals("simple body", localResponse.body?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When mocked file does not exist, then the request should go to the server")
        fun `should support mixed mode to execute request when response file is not found locally`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            enqueueServerResponse(REQUEST_OK_CODE, "body")
            setUpInterceptor(MIXED, mapper, type)
            whenever(loadingLambda.invoke(any())).then { throw FileNotFoundException("File does not exist") }
            val serverResponse = executeGetRequest("")

            assertResponseCode(serverResponse, REQUEST_OK_CODE, REQUEST_OK_MESSAGE)
            assertEquals("body", serverResponse.body?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When several interceptors are stacked, " +
                    "then each should delegate to the next one requests it can't answer"
        )
        fun `should allow to stack several interceptors thanks to mixed mode`(
            title: String,
            mapper: Mapper,
            type: String
        ) {
            initFilingPolicy(type)

            enqueueServerResponse(REQUEST_OK_CODE, "server response")

            val inMemoryInterceptor =
                mockInterceptor {
                    useDynamicMocks { request ->
                        ResponseDescriptor(
                            code = REQUEST_OK_CODE,
                            body = "in memory response",
                            mediaType = "text/plain"
                        ).takeIf {
                            request.url.encodedPath == "/inMemory" && request.method == "GET"
                        }
                    }
                    setInterceptorStatus(MIXED)
                }

            val fileBasedInterceptor =
                mockInterceptor {
                    decodeScenarioPathWith(filingPolicy)
                    loadFileWith(loadingLambda)
                    parseScenariosWith(mapper)
                    setInterceptorStatus(MIXED)
                }

            client = OkHttpClient.Builder()
                .addInterceptor(inMemoryInterceptor)
                .addInterceptor(fileBasedInterceptor)
                .build()

            assertEquals("in memory response", executeGetRequest("inMemory").body?.string())
            assertEquals("file response", executeGetRequest("fileMatch").body?.string())
            assertEquals("server response", executeGetRequest("serverMatch").body?.string())
        }
    }

    private fun setUpInterceptor(
        mode: Mode,
        mapper: Mapper,
        type: String,
        delay: Long? = null
    ) {
        initFilingPolicy(type)

        interceptor = mockInterceptor {
            decodeScenarioPathWith(filingPolicy)
            loadFileWith(loadingLambda)
            parseScenariosWith(mapper)
            setInterceptorStatus(mode)
            delay?.let { addFakeNetworkDelay(it) }
        }

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    private fun initFilingPolicy(fileType: String) {
        filingPolicy = mock {
            on { getPath(any()) } doAnswer {
                val path = it.getArgument<Request>(0).url.encodedPath
                ("$path.$fileType").drop(1)
            }
        }
    }

    companion object {
        private const val SINGLE_FILE = "single_file"

        private fun loadingError(): Nothing {
            error("Loading error")
        }
    }
}
