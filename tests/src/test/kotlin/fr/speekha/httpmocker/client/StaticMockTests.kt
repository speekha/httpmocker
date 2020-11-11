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

package fr.speekha.httpmocker.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.Mode.ENABLED
import fr.speekha.httpmocker.Mode.MIXED
import fr.speekha.httpmocker.assertThrows
import fr.speekha.httpmocker.client.TestWithServer.Companion.REQUEST_OK_CODE
import fr.speekha.httpmocker.client.TestWithServer.Companion.REQUEST_SIMPLE_BODY
import fr.speekha.httpmocker.client.TestWithServer.Companion.URL_HEADERS
import fr.speekha.httpmocker.client.TestWithServer.Companion.URL_METHOD
import fr.speekha.httpmocker.client.TestWithServer.Companion.URL_SIMPLE_REQUEST
import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.SingleFilePolicy
import fr.speekha.httpmocker.serialization.Mapper
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.system.measureTimeMillis

@Suppress("UNUSED_PARAMETER")
@DisplayName("Static Mocks with Ktor")
abstract class StaticMockTests<Response, Client> : HttpClientTester<Response, Client> {

    protected val loadingLambda: (String) -> InputStream? = mock {
        on { invoke(any()) } doAnswer { javaClass.classLoader.getResourceAsStream(it.getArgument(0)) }
    }

    protected lateinit var filingPolicy: FilingPolicy

    @Nested
    @DisplayName("Given an enabled mock interceptor with static scenarios")
    inner class StaticTests {
        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When a request is answered with a scenario, " +
                "then its path should be computed by the file policy"
        )
        fun `should delegate path resolutions`(title: String, mapper: Mapper, type: String) {
            runBlocking {
                setUpInterceptor(ENABLED, mapper, type)
                executeRequest(URL_SIMPLE_REQUEST)

                verify(filingPolicy).getPath(argThat { this.path == URL_SIMPLE_REQUEST })
            }
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When no scenario file is provided, " +
                "then a 404 error should occur"
        )
        fun `should return a 404 error when response is not found`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            check404Response("/unknown")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When no requests in the file matches, " +
                "then a 404 error should occur"
        )
        fun `should return a 404 error when no request matches the criteria`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            whenever(loadingLambda.invoke(any())) doAnswer {
                loadingError()
            }
            setUpInterceptor(ENABLED, mapper, type)

            check404Response("/no_match")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When an error occurs while loading scenarios, " +
                "then a 404 error should occur"
        )
        fun `should return a 404 error when an exception occurs`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            whenever(loadingLambda.invoke(any())) doAnswer {
                loadingError()
            }
            setUpInterceptor(ENABLED, mapper, type)

            check404Response("/unknown")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When loaded scenario has neither response nor error, " +
                "then a 404 error should occur"
        )
        fun `should return a 404 error when the matching request has no response or error`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            check404Response("/no_response_no_error")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When an error is configured as an answer, " +
                "then the corresponding exception should be thrown"
        )
        fun `should throw a mocked exception`(title: String, mapper: Mapper, type: String) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            val exception = assertThrows<IOException> {
                executeRequest("/exception")
            }
            assertNull(exception.message)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When an error with a message is configured as an answer, " +
                "then the corresponding exception with message should be thrown"
        )
        fun `should throw a mocked exception with message`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            val exception = assertThrows<IOException> {
                executeRequest("/exception_with_message")
            }
            assertEquals("An exception message", exception.message)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When an error occurs while answering a request, " +
                "then the request body should be the error"
        )
        fun `should return the error message when an exception occurs`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            whenever(loadingLambda.invoke(any())) doAnswer {
                loadingError()
            }
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeRequest("/unknown")

            assertResponseBodyStartsWith(
                "java.lang.IllegalStateException: Loading error\n" +
                    "\tat fr.speekha.httpmocker.client.",
                response
            )
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a response is found, then default HTTP code should be 200")
        fun `should return a 200 when response is found`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeRequest(URL_SIMPLE_REQUEST)

            assertResponseCode(HttpStatusCode.OK, response)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When a response is found and response body is in JSON scenario, " +
                "then it should be loaded from scenario"
        )
        fun `should return a predefined response body from json descriptor`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody(REQUEST_SIMPLE_BODY, URL_SIMPLE_REQUEST)
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor with static and dynamic mocks")
    inner class StaticAndDynamic {
        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When a request is answered, " +
                "then dynamic mocks should be tried first before static ones"
        )
        fun `should support dynamic and static mocks together`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            val result1 = "Dynamic"
            val result2 = REQUEST_SIMPLE_BODY

            initFilingPolicy(type)

            setupStaticConf(ENABLED, loadingLambda, mapper, null, filingPolicy) { request ->
                ResponseDescriptor(body = result1).takeIf {
                    request.path.contains("dynamic")
                }
            }

            checkResponseBody(result1, "http://www.test.fr/dynamic")
            checkResponseBody(result2, "http://www.test.fr/request")
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
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        fun `should return a predefined response body from separate file`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("separate body file", "/body_file")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When scenario file path is not empty, " +
                "then response body should be in the same folder by default"
        )
        fun `should return a predefined response body from separate file in the same folder`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("separate body file", "/folder/request_in_folder")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When response body is in a child folder, " +
                "then response body path should be read from that folder"
        )
        fun `should return a predefined response body from separate file in a different folder`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("separate body file", "/request_in_other_folder")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When response body is in a parent folder, " +
                "then response body path should be read from that folder"
        )
        fun `should return a predefined response body from separate file in a parent folder`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("separate body file", "/folder2/request_in_other_folder")
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor and a response scenario")
    inner class ResponseBuilding {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When a request is answered, then the proper " +
                "headers should be set in the response"
        )
        fun `should return proper headers`(title: String, mapper: Mapper, type: String) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeRequest(URL_SIMPLE_REQUEST)

            assertResponseCode(HttpStatusCode.OK, response)
            assertHeaderEquals("simple header", response, "testHeader")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When the response is a redirect, " +
                "then the response should have a HTTP code 302 and a location"
        )
        fun `should handle redirects`(title: String, mapper: Mapper, type: String) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeRequest("/redirect")

            assertResponseCode(HttpStatusCode.Found, response)
            assertHeaderEquals("http://www.google.com", response, "Location")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When a request is answered, " +
                "then the proper content type should be set in the response"
        )
        fun `should handle media type`(title: String, mapper: Mapper, type: String) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            val response = executeRequest("/mediatype")

            assertResponseCode(HttpStatusCode.OK, response)
            assertContentType("application", "json", response)
            assertHeaderEquals("application/json", response, "Content-type")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When a request with no specific delay is answered, " +
                "then default delay should be used"
        )
        fun `should allow to delay all responses`(title: String, mapper: Mapper, type: String) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type, 50)

            val delay = measureTimeMillis {
                executeRequest(URL_SIMPLE_REQUEST)
            }

            val threshold = 50
            assertTrue(delay >= threshold, "Time was $delay (< $threshold ms)")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When a request with a specific delay is answered, " +
                "then that delay should be used"
        )
        fun `should allow to delay responses based on configuration`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            val delay = measureTimeMillis {
                executeRequest("/delay")
            }

            val noDelay = measureTimeMillis {
                executeRequest(URL_SIMPLE_REQUEST)
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
            setupStaticConf(ENABLED, loadingLambda, mapper)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should use a MirrorPathPolicy as default")
        fun `should use mirror path as default file policy`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setupInterceptor(mapper, type)

            checkResponseBody(REQUEST_SIMPLE_BODY, URL_SIMPLE_REQUEST)
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor and a single file policy")
    inner class UrlMatching {

        fun setupInterceptor(scenarioFile: String, mapper: Mapper, type: String) {
            setupStaticConf(ENABLED, loadingLambda, mapper, null, SingleFilePolicy("$scenarioFile.$type"))
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match URL protocol")
        fun `should take http protocol into account`(title: String, mapper: Mapper, type: String) = runBlocking {
            setupInterceptor("protocol", mapper, type)

            checkResponseBody("HTTP", "/protocol")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match the URL host")
        fun `should select response based on host`(title: String, mapper: Mapper, type: String) = runBlocking {
            setupInterceptor(SINGLE_FILE, mapper, type)

            checkResponseBody("based on host", "http://hostTest.com:12345/anyUrl")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match the URL port")
        fun `should select response based on port`(title: String, mapper: Mapper, type: String) = runBlocking {
            setupInterceptor(SINGLE_FILE, mapper, type)

            checkResponseBody("based on port", "http://someHost.com:45612/anyUrl")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match the URL path")
        fun `should select response based on URL path`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setupInterceptor(SINGLE_FILE, mapper, type)

            checkResponseBody("based on URL", "http://someHost.com:12345/aTestUrl")
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

            setupStaticConf(ENABLED, loadingLambda, mapper, null, policy1, policy2)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When the first policy provides a match, then it should be used")
        fun `should use first policy if possible`(title: String, mapper: Mapper, type: String) = runBlocking {
            setupInterceptor(mapper, type)
            whenever(policy1.getPath(any())).thenReturn("request.$type")

            checkResponseBody(REQUEST_SIMPLE_BODY, URL_SIMPLE_REQUEST)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When the first policy does not provide a match, then the second one should be used")
        fun `should use second policy as fallback`(title: String, mapper: Mapper, type: String) = runBlocking {
            setupInterceptor(mapper, type)
            whenever(policy2.getPath(any())).thenReturn("request.$type")
            checkResponseBody(REQUEST_SIMPLE_BODY, URL_SIMPLE_REQUEST)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When none of the policies provide a match, then an 404 error should occur")
        fun `should handle all policy failure`(title: String, mapper: Mapper, type: String) = runBlocking {
            setupInterceptor(mapper, type)
            check404Response(URL_SIMPLE_REQUEST)
        }
    }

    @Nested
    @DisplayName("Given an enabled mock interceptor and a scenario with multiple request patterns")
    inner class RequestMatching {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match the HTTP method")
        fun `should take http method into account`(title: String, mapper: Mapper, type: String) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("get", URL_METHOD)
            checkResponseBody("post", URL_METHOD, "POST", "")
            checkResponseBody("put", URL_METHOD, "PUT", "")
            checkResponseBody("delete", URL_METHOD, "DELETE", "")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match present query parameters")
        fun `should select response based on query params`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("param A", "/query_param?param=1")
            checkResponseBody("param B", "/query_param?param=2")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match absent query parameters")
        fun `should select response based on absent query params`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("Body found", "/absent_query_param?param1=1")
            check404Response("/absent_query_param?param1=1&param2=2")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match present headers")
        fun `should select response based on headers`(title: String, mapper: Mapper, type: String) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            val noHeaders = executeRequest(URL_HEADERS)
            val headers = executeRequest(
                URL_HEADERS,
                headers = listOf(
                    "header1" to "1",
                    "header1" to "2",
                    "header2" to "3"
                )
            )
            val header1 = executeRequest(
                URL_HEADERS,
                headers = listOf("header1" to "1")
            )
            val header2 = executeRequest(
                URL_HEADERS,
                headers = listOf("header2" to "2")
            )

            assertResponseBody("no header", noHeaders)
            assertResponseBody("with header 1", header1)
            assertResponseBody("with header 2", header2)
            assertResponseBody("with headers", headers)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match absent headers")
        fun `should select response based on absent headers`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("Body found", "/absent_header", headers = listOf("header1" to "1"))
            check404Response("/absent_header", headers = listOf("header1" to "1", "header2" to "2"))
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is answered, then it should match request body")
        fun `should select response based on request body`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("matched", "/body_matching", "POST", "azer1zere")
            checkResponseBody("no match", "/body_matching", "POST", "azerzere")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When a request with exact match is answered, " +
                "then match should not allow extra headers or parameters"
        )
        fun `should select response based on exact matches`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            setUpInterceptor(ENABLED, mapper, type)

            checkResponseBody("Exact headers", "/exact_match", headers = listOf("header1" to "1"))
            check404Response("/exact_match_ktor", headers = listOf("header1" to "1", "header2" to "2"))
            checkResponseBody("Exact params", "/exact_match?param1=1")
            check404Response("/exact_match_ktor?param1=1&param2=2")
        }
    }

    @Nested
    @DisplayName("Given a mock interceptor in mixed mode")
    inner class MixedMode {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is not mocked, then it should go to the server")
        fun `should support mixed mode to execute request when no response is found locally`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            enqueueServerResponseTmp(REQUEST_OK_CODE, "body")
            setUpInterceptor(MIXED, mapper, type)

            checkResponseBody("body", "")
            checkResponseBody(REQUEST_SIMPLE_BODY, URL_SIMPLE_REQUEST)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When mocked file does not exist, then the request should go to the server")
        fun `should support mixed mode to execute request when response file is not found locally`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            enqueueServerResponseTmp(REQUEST_OK_CODE, "body")
            setUpInterceptor(MIXED, mapper, type)
            whenever(loadingLambda.invoke(any())).then { throw FileNotFoundException("File does not exist") }
            checkResponseBody("body", "")
        }
    }

    private fun setUpInterceptor(
        mode: Mode,
        mapper: Mapper,
        type: String,
        delay: Long? = null
    ) {
        initFilingPolicy(type)

        this.setupStaticConf(mode, loadingLambda, mapper, delay, filingPolicy)
    }

    protected fun initFilingPolicy(fileType: String) {
        filingPolicy = mock {
            on { getPath(any()) } doAnswer {
                val path = it.getArgument<HttpRequest>(0).path
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
