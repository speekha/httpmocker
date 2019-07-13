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

package fr.speekha.httpmocker

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.speekha.httpmocker.MockResponseInterceptor.Mode.*
import fr.speekha.httpmocker.custom.CustomMapper
import fr.speekha.httpmocker.gson.GsonMapper
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.kotlinx.JsonFormatConverter
import fr.speekha.httpmocker.kotlinx.KotlinxMapper
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.moshi.MoshiMapper
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.InMemoryPolicy
import fr.speekha.httpmocker.policies.SingleFilePolicy
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.stream.Stream
import kotlin.system.measureTimeMillis

class StaticMockTests {

    private val server = MockWebServer()

    private val mockServerBaseUrl: String
        get() = "http://127.0.0.1:${server.port}"

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

    private lateinit var interceptor: MockResponseInterceptor

    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
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

        val request = buildRequest("http://someHost.com:12345/aTestUrl")
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

        val request = buildRequest("http://hostTest.com:12345/anyUrl")
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

        val request = buildRequest("http://someHost.com:45612/anyUrl")
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
    fun `should let requests through when recording`(title: String, mapper: Mapper) {
        enqueueServerResponse(200, "body")
        setUpInterceptor(RECORD, mapper, SAVE_FOLDER)

        val response = executeGetRequest("record/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should let requests through when recording even if saving fails`(
        title: String,
        mapper: Mapper
    ) {
        enqueueServerResponse(200, "body")
        setUpInterceptor(RECORD, mapper, "")

        val response = executeGetRequest("record/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should store requests and responses in the proper locations when recording`(
        title: String,
        mapper: Mapper
    ) {
        enqueueServerResponse(200, "body")
        setUpInterceptor(RECORD, mapper, SAVE_FOLDER)

        executeGetRequest("record/request")

        assertFileExists("$SAVE_FOLDER/record/request.json")
        assertFileExists("$SAVE_FOLDER/record/request_body_0.txt")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should store requests and responses when recording`(title: String, mapper: Mapper) {
        enqueueServerResponse(200, "body", listOf("someKey" to "someValue"))
        setUpInterceptor(RECORD, mapper, SAVE_FOLDER)

        executeRequest(
            "request?param1=value1",
            "POST",
            "requestBody",
            listOf("someHeader" to "someValue")
        )

        withFile("$SAVE_FOLDER/request.json") {
            val result: List<Matcher> =
                mapper.readMatches(it)
            val expectedResult = Matcher(
                RequestDescriptor(
                    method = "POST",
                    body = "requestBody",
                    params = mapOf("param1" to "value1"),
                    headers = listOf(Header("someHeader", "someValue"))
                ),
                ResponseDescriptor(
                    code = 200,
                    bodyFile = "request_body_0.txt",
                    mediaType = "text/plain",
                    headers = listOf(
                        Header("Content-Length", "4"),
                        Header("Content-Type", "text/plain"),
                        Header("someKey", "someValue")
                    )
                )
            )
            assertEquals(listOf(expectedResult), result)
        }

        withFile("$SAVE_FOLDER/request_body_0.txt") {
            assertEquals("body", it.readAsString())
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should handle null request and response bodies when recording`(
        title: String,
        mapper: Mapper
    ) {
        enqueueServerResponse(200, null)
        setUpInterceptor(RECORD, mapper, SAVE_FOLDER)

        executeRequest("request", "GET", null)

        withFile("$SAVE_FOLDER/request.json") {
            val result: List<Matcher> =
                mapper.readMatches(it)
            val expectedResult = Matcher(
                RequestDescriptor(method = "GET"),
                ResponseDescriptor(
                    code = 200,
                    mediaType = "text/plain",
                    headers = listOf(
                        Header("Content-Length", "0"),
                        Header("Content-Type", "text/plain")
                    )
                )
            )
            assertEquals(listOf(expectedResult), result)
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should update existing descriptors when recording`(title: String, mapper: Mapper) {
        enqueueServerResponse(200, "body", listOf("someKey" to "someValue"))
        enqueueServerResponse(200, "second body")
        setUpInterceptor(RECORD, mapper, SAVE_FOLDER)

        executeRequest(
            "request?param1=value1",
            "POST",
            "requestBody",
            listOf("someHeader" to "someValue")
        )
        executeGetRequest("request")

        withFile("$SAVE_FOLDER/request.json") {
            val result: List<Matcher> =
                mapper.readMatches(it)
            val expectedResult = listOf(
                Matcher(
                    RequestDescriptor(
                        method = "POST",
                        body = "requestBody",
                        params = mapOf("param1" to "value1"),
                        headers = listOf(Header("someHeader", "someValue"))
                    ),
                    ResponseDescriptor(
                        code = 200,
                        bodyFile = "request_body_0.txt",
                        mediaType = "text/plain",
                        headers = listOf(
                            Header("Content-Length", "4"),
                            Header("Content-Type", "text/plain"),
                            Header("someKey", "someValue")
                        )
                    )
                ),
                Matcher(
                    RequestDescriptor(method = "GET"),
                    ResponseDescriptor(
                        code = 200,
                        bodyFile = "request_body_1.txt",
                        mediaType = "text/plain",
                        headers = listOf(
                            Header("Content-Length", "11"),
                            Header("Content-Type", "text/plain")
                        )
                    )
                )
            )
            assertEquals(expectedResult, result)
        }

        withFile("$SAVE_FOLDER/request_body_0.txt") {
            assertEquals("body", it.readAsString())
        }
        withFile("$SAVE_FOLDER/request_body_1.txt") {
            assertEquals("second body", it.readAsString())
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should add proper extension to response files`(title: String, mapper: Mapper) {
        enqueueServerResponse(200, "body", contentType = "image/png")
        enqueueServerResponse(200, "body", contentType = "application/json")
        setUpInterceptor(RECORD, mapper, SAVE_FOLDER)

        executeGetRequest("record/request1")
        executeGetRequest("record/request2")

        assertFileExists("$SAVE_FOLDER/record/request1_body_0.png")
        assertFileExists("$SAVE_FOLDER/record/request2_body_0.json")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should match indexes in descriptor file and actual response file name`(
        title: String,
        mapper: Mapper
    ) {
        enqueueServerResponse(200, "body", contentType = "image/png")
        enqueueServerResponse(200, "body", contentType = "application/json")
        setUpInterceptor(RECORD, mapper, SAVE_FOLDER)

        executeGetRequest("record/request")
        executeGetRequest("record/request")

        assertFileExists("$SAVE_FOLDER/record/request_body_0.png")
        assertFileExists("$SAVE_FOLDER/record/request_body_1.json")
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
            client.newCall(buildRequest("http://www.test.fr/dynamic", method = "GET")).execute()
        val response2 =
            client.newCall(buildRequest("http://www.test.fr/request", method = "GET")).execute()

        assertEquals(result1, response1.body()?.string())
        assertEquals(result2, response2.body()?.string())
    }


    private fun File.readAsString() = FileInputStream(this).readAsString()

    private fun assertFileExists(path: String) = withFile(path) {
        assertTrue(it.exists())
    }

    private fun <T : Any?> withFile(path: String, block: (File) -> T) = block(File(path))

    @AfterEach
    fun clearFolder() {
        val folder = File(SAVE_FOLDER)
        if (folder.exists()) {
            Files.walk(folder.toPath())
                .sorted(Collections.reverseOrder<Any>())
                .map(Path::toFile)
                .forEach { it.delete() }
        }
    }

    private fun setUpInterceptor(
        mode: MockResponseInterceptor.Mode,
        mapper: Mapper? = null,
        rootFolder: String? = null
    ) {
        interceptor = MockResponseInterceptor.Builder()
            .decodeScenarioPathWith(filingPolicy)
            .loadFileWith(loadingLambda)
            .apply {
                if (mapper != null) {
                    parseScenariosWith(mapper)
                }
                if (rootFolder != null) {
                    saveScenariosIn(File(rootFolder))
                }
            }
            .setInterceptorStatus(mode)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    private fun assertResponseCode(response: Response, code: Int, message: String) {
        assertEquals(code, response.code())
        assertEquals(message, response.message())
    }

    private fun enqueueServerResponse(
        responseCode: Int,
        responseBody: String?,
        headers: List<Pair<String, String>> = listOf(),
        contentType: String? = null
    ) {
        val serverResponse = MockResponse().apply {
            setResponseCode(responseCode)
            if (responseBody != null) {
                setBody(responseBody)
            }
            addHeader("Content-Type", contentType ?: "text/plain")
            headers.forEach { addHeader(it.first, it.second) }
        }
        server.enqueue(serverResponse)
    }

    private fun executeGetRequest(
        url: String,
        headers: List<Pair<String, String>> = emptyList()
    ): Response =
        executeRequest(url, "GET", null, headers)

    private fun executeRequest(
        url: String,
        method: String,
        body: String?,
        headers: List<Pair<String, String>> = emptyList()
    ): Response {
        val request = initRequest(url, headers, method, body)
        return client.newCall(request).execute()
    }

    private fun initRequest(
        url: String,
        headers: List<Pair<String, String>> = emptyList(),
        method: String = "GET",
        body: String? = null
    ): Request {
        val path = if (url.startsWith("/")) url.drop(1) else url
        return buildRequest("$mockServerBaseUrl/$path", headers, method, body)
    }


    companion object {
        private const val SAVE_FOLDER = "testFolder"

        @JvmStatic
        fun data(): Stream<Arguments> = Stream.of(
            Arguments.of("Jackson", JacksonMapper()),
            Arguments.of("Gson", GsonMapper()),
            Arguments.of("Moshi", MoshiMapper()),
            Arguments.of("Custom mapper", CustomMapper()),
            Arguments.of(
                "Kotlinx serialization",
                KotlinxMapper(JsonFormatConverter()::import, JsonFormatConverter()::export)
            )
        )
    }
}