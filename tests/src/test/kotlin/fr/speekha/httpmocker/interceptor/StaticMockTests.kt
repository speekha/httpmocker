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
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.MockResponseInterceptor.Mode.*
import fr.speekha.httpmocker.buildRequest
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.InMemoryPolicy
import fr.speekha.httpmocker.policies.SingleFilePolicy
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.InputStream
import java.util.stream.Stream
import kotlin.system.measureTimeMillis

class StaticMockTests : TestWithServer() {

    private val loadingLambda: (String) -> InputStream? = mock {
        on { invoke(any()) } doAnswer { javaClass.classLoader.getResourceAsStream(it.getArgument(0)) }
    }

    private val filingPolicy: FilingPolicy = mock {
        on { getPath(any()) } doAnswer {
            (it.getArgument<Request>(0).url().encodedPath() + ".json").drop(
                1
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should not interfere with requests when disabled`(title: String, mapper: Mapper) {
        setUpInterceptor(DISABLED, mapper)
        enqueueServerResponse(200, "body")

        val response = executeGetRequest("")

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return a 404 error when response is not found`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/unknown")

        assertResponseCode(response, 404, "Not Found")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return a 404 error when an exception occurs`(title: String, mapper: Mapper) {
        whenever(loadingLambda.invoke(any())) doAnswer {
            error("Loading error")
        }
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/unknown")

        assertResponseCode(response, 404, "Not Found")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return a 404 error when no response matches the criteria`(
        title: String,
        mapper: Mapper
    ) {
        whenever(loadingLambda.invoke(any())) doAnswer {
            error("Loading error")
        }
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/no_match")

        assertResponseCode(response, 404, "Not Found")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return a 200 when response is found`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/request")

        assertResponseCode(response, 200, "OK")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return a predefined response body from json descriptor`(
        title: String,
        mapper: Mapper
    ) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple body", response.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return a predefined response body from separate file`(
        title: String,
        mapper: Mapper
    ) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/body_file")

        assertResponseCode(response, 200, "OK")
        assertEquals("separate body file", response.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return a predefined response body from separate file in the same folder`(
        title: String,
        mapper: Mapper
    ) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/folder/request_in_folder")

        assertResponseCode(response, 200, "OK")
        assertEquals("separate body file", response.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return a predefined response body from separate file in a different folder`(
        title: String,
        mapper: Mapper
    ) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/request_in_other_folder")

        assertResponseCode(response, 200, "OK")
        assertEquals("separate body file", response.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return a predefined response body from separate file in a parent folder`(
        title: String,
        mapper: Mapper
    ) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/folder2/request_in_other_folder")

        assertResponseCode(response, 200, "OK")
        assertEquals("separate body file", response.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should return proper headers`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple header", response.header("testHeader"))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should handle redirects`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/redirect")

        assertResponseCode(response, 302, "Found")
        assertEquals("http://www.google.com", response.header("Location"))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should handle media type`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/mediatype")

        assertResponseCode(response, 200, "OK")
        assertEquals("application", response.body()?.contentType()?.type())
        assertEquals("application/json", response.header("Content-type"))
        assertEquals("json", response.body()?.contentType()?.subtype())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should select response based on query params`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val param1 = executeGetRequest("/query_param?param=1").body()?.string()
        val param2 = executeGetRequest("/query_param?param=2").body()?.string()

        assertEquals("param A", param1)
        assertEquals("param B", param2)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should select response based on URL path`(title: String, mapper: Mapper) {
        val policy = SingleFilePolicy("single_file.json")
        val interceptor = MockResponseInterceptor.Builder()
            .decodeScenarioPathWith(policy)
            .loadFileWith(loadingLambda)
            .parseScenariosWith(mapper)
            .setInterceptorStatus(ENABLED)
            .build()

        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request =
            buildRequest("http://someHost.com:12345/aTestUrl")
        assertEquals("based on URL", client.newCall(request).execute().body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should select response based on host`(title: String, mapper: Mapper) {
        val policy = SingleFilePolicy("single_file.json")
        val interceptor = MockResponseInterceptor.Builder()
            .decodeScenarioPathWith(policy)
            .loadFileWith(loadingLambda)
            .parseScenariosWith(mapper)
            .setInterceptorStatus(ENABLED)
            .build()

        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request =
            buildRequest("http://hostTest.com:12345/anyUrl")
        assertEquals("based on host", client.newCall(request).execute().body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should select response based on port`(title: String, mapper: Mapper) {
        val policy = SingleFilePolicy("single_file.json")
        val interceptor = MockResponseInterceptor.Builder()
            .decodeScenarioPathWith(policy)
            .loadFileWith(loadingLambda)
            .parseScenariosWith(mapper)
            .setInterceptorStatus(ENABLED)
            .build()

        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request =
            buildRequest("http://someHost.com:45612/anyUrl")
        assertEquals("based on port", client.newCall(request).execute().body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should select response based on headers`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val param1 = executeGetRequest("/headers").body()?.string()
        val param2 = executeGetRequest(
            "/headers",
            listOf(
                "header1" to "1",
                "header1" to "2",
                "header2" to "3"
            )
        ).body()?.string()

        assertEquals("no header", param1)
        assertEquals("with headers", param2)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should take http protocol into account`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val get = executeGetRequest("/protocol").body()?.string()

        assertEquals("HTTP", get)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should take http method into account`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val get = executeGetRequest("/method").body()?.string()
        val post = executeRequest("/method", "POST", "").body()?.string()
        val put = executeRequest("/method", "PUT", "").body()?.string()
        val delete = executeRequest("/method", "DELETE", "").body()?.string()

        assertEquals("get", get)
        assertEquals("post", post)
        assertEquals("put", put)
        assertEquals("delete", delete)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should select response based on request body`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val match = executeRequest("/body_matching", "POST", "azer1zere").body()?.string()
        val noMatch = executeRequest("/body_matching", "POST", "azerzere").body()?.string()

        assertEquals("matched", match)
        assertEquals("no match", noMatch)
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should select response based on exact matches`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val response = executeGetRequest("/exact_match?param1=1&param2=2")

        assertEquals(404, response.code())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should allow to delay all responses`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)
        interceptor.delay = 50

        val delay = measureTimeMillis {
            executeGetRequest("/request").body()?.string()
        }

        val threshold = 50
        assertTrue(delay >= threshold, "Time was $delay (< $threshold ms)")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should allow to delay responses based on configuration`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val delay = measureTimeMillis {
            executeGetRequest("/delay").body()?.string()
        }

        val noDelay = measureTimeMillis {
            executeGetRequest("/request").body()?.string()
        }

        val threshold = 50
        assertTrue(delay >= threshold, "Time was $delay (< $threshold ms)")
        assertTrue(noDelay < threshold, "Time without delay was $noDelay (> $threshold ms)")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should delegate path resolutions`(title: String, mapper: Mapper) {
        setUpInterceptor(ENABLED, mapper)

        val request = initRequest("/request")
        client.newCall(request).execute()

        verify(filingPolicy).getPath(request)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should support mixed mode to execute request when no response is found locally`(
        title: String,
        mapper: Mapper
    ) {
        enqueueServerResponse(200, "body")
        setUpInterceptor(MIXED, mapper)

        val serverResponse = executeGetRequest("")
        val localResponse = executeGetRequest("/request")

        assertResponseCode(serverResponse, 200, "OK")
        assertEquals("body", serverResponse.body()?.string())
        assertResponseCode(localResponse, 200, "OK")
        assertEquals("simple body", localResponse.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should allow to stack several interceptors thanks to mixed mode`(
        title: String,
        mapper: Mapper
    ) {
        enqueueServerResponse(200, "server response")

        val inMemoryPolicy = InMemoryPolicy(mapper)
        inMemoryPolicy.addMatcher(
            "$mockServerBaseUrl/inMemory", Matcher(
                RequestDescriptor(method = "GET"),
                ResponseDescriptor(
                    code = 200,
                    body = "in memory response",
                    mediaType = "text/plain"
                )
            )
        )
        val inMemoryInterceptor = MockResponseInterceptor.Builder()
            .decodeScenarioPathWith(inMemoryPolicy)
            .loadFileWith(inMemoryPolicy::matchRequest)
            .parseScenariosWith(mapper)
            .setInterceptorStatus(MIXED)
            .build()

        val fileBasedInterceptor = MockResponseInterceptor.Builder()
            .decodeScenarioPathWith(filingPolicy)
            .loadFileWith(loadingLambda)
            .parseScenariosWith(mapper)
            .setInterceptorStatus(MIXED)
            .build()

        client = OkHttpClient.Builder()
            .addInterceptor(inMemoryInterceptor)
            .addInterceptor(fileBasedInterceptor)
            .build()

        assertEquals("in memory response", executeGetRequest("inMemory").body()?.string())
        assertEquals("file response", executeGetRequest("fileMatch").body()?.string())
        assertEquals("server response", executeGetRequest("serverMatch").body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should support dynamic and static mocks together`(title: String, mapper: Mapper) {
        val result1 = "Dynamic"
        val result2 = "simple body"

        interceptor = MockResponseInterceptor.Builder()
            .useDynamicMocks {
                if (it.url().toString().contains("dynamic"))
                    ResponseDescriptor(body = result1)
                else null
            }
            .decodeScenarioPathWith(filingPolicy)
            .loadFileWith(loadingLambda)
            .parseScenariosWith(mapper)
            .setInterceptorStatus(ENABLED)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val response1 =
            client.newCall(
                buildRequest(
                    "http://www.test.fr/dynamic",
                    method = "GET"
                )
            ).execute()
        val response2 =
            client.newCall(
                buildRequest(
                    "http://www.test.fr/request",
                    method = "GET"
                )
            ).execute()

        assertEquals(result1, response1.body()?.string())
        assertEquals(result2, response2.body()?.string())
    }

    private fun setUpInterceptor(mode: MockResponseInterceptor.Mode, mapper: Mapper) {
        interceptor = MockResponseInterceptor.Builder()
            .decodeScenarioPathWith(filingPolicy)
            .loadFileWith(loadingLambda)
            .parseScenariosWith(mapper)
            .setInterceptorStatus(mode)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    companion object {
        @JvmStatic
        fun data(): Stream<Arguments> =
            mappers
    }
}