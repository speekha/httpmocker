package fr.speekha.httpmocker

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.nhaarman.mockitokotlin2.*
import fr.speekha.httpmocker.MockResponseInterceptor.MODE.*
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.InMemoryPolicy
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.system.measureTimeMillis

class MockInterceptorTest {

    private val server = MockWebServer()

    private val mockServerBaseUrl: String
        get() = "http://127.0.0.1:${server.port}"

    private val loadingLambda: (String) -> InputStream? = mock {
        on { invoke(any()) } doAnswer { javaClass.classLoader.getResourceAsStream(it.getArgument(0)) }
    }

    private val filingPolicy: FilingPolicy = mock {
        on { getPath(any()) } doAnswer { (it.getArgument<Request>(0).url().encodedPath() + ".json").drop(1) }
    }

    private lateinit var interceptor: MockResponseInterceptor

    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
    }

    @Test
    fun `should not interfere with requests when disabled`() {
        setUpInterceptor(DISABLED)
        enqueueServerResponse(200, "body")

        val response = executeGetRequest("")

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @Test
    fun `should return a 404 error when response is not found`() {
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/unknown")

        assertResponseCode(response, 404, "Not Found")
    }

    @Test
    fun `should return a 404 error when an exception occurs`() {
        whenever(loadingLambda.invoke(any())) doAnswer {
            error("Loading error")
        }
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/unknown")

        assertResponseCode(response, 404, "Not Found")
    }

    @Test
    fun `should return a 404 error when no response matches the criteria`() {
        whenever(loadingLambda.invoke(any())) doAnswer {
            error("Loading error")
        }
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/no_match")

        assertResponseCode(response, 404, "Not Found")
    }

    @Test
    fun `should return a 200 when response is found`() {
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/request")

        assertResponseCode(response, 200, "OK")
    }

    @Test
    fun `should return a predefined response body from json descriptor`() {
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple body", response.body()?.string())
    }

    @Test
    fun `should return a predefined response body from separate file`() {
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/body_file")

        assertResponseCode(response, 200, "OK")
        assertEquals("separate body file", response.body()?.string())
    }

    @Test
    fun `should return a predefined response body from separate file in the same folder`() {
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/folder/request_in_folder")

        assertResponseCode(response, 200, "OK")
        assertEquals("separate body file", response.body()?.string())
    }

    @Test
    fun `should return proper headers`() {
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple header", response.header("testHeader"))
    }

    @Test
    fun `should handle redirects`() {
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/redirect")

        assertResponseCode(response, 302, "Found")
        assertEquals("http://www.google.com", response.header("Location"))
    }

    @Test
    fun `should handle media type`() {
        setUpInterceptor(ENABLED)

        val response = executeGetRequest("/mediatype")

        assertResponseCode(response, 200, "OK")
        assertEquals("application", response.body()?.contentType()?.type())
        assertEquals("json", response.body()?.contentType()?.subtype())
    }

    @Test
    fun `should select response based on query params`() {
        setUpInterceptor(ENABLED)

        val param1 = executeGetRequest("/query_param?param=1").body()?.string()
        val param2 = executeGetRequest("/query_param?param=2").body()?.string()

        assertEquals("param A", param1)
        assertEquals("param B", param2)
    }

    @Test
    fun `should select response based on headers`() {
        setUpInterceptor(ENABLED)

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

    @Test
    fun `should take http method into account`() {
        setUpInterceptor(ENABLED)

        val get = executeGetRequest("/method").body()?.string()
        val post = executeRequest("/method", "POST", "").body()?.string()
        val put = executeRequest("/method", "PUT", "").body()?.string()
        val delete = executeRequest("/method", "DELETE", "").body()?.string()

        assertEquals("get", get)
        assertEquals("post", post)
        assertEquals("put", put)
        assertEquals("delete", delete)
    }


    @Test
    fun `should select response based on request body`() {
        setUpInterceptor(ENABLED)

        val match = executeRequest("/body_matching", "POST", "azer1zere").body()?.string()
        val noMatch = executeRequest("/body_matching", "POST", "azerzere").body()?.string()

        assertEquals("matched", match)
        assertEquals("no match", noMatch)
    }

    @Test
    fun `should allow to delay all responses`() {
        setUpInterceptor(ENABLED)
        interceptor.delay = 50

        val delay = measureTimeMillis {
            executeGetRequest("/request").body()?.string()
        }

        val threshold = 50
        assertTrue(delay > threshold, "Time was $delay (< $threshold ms)")
    }

    @Test
    fun `should allow to delay responses based on configuration`() {
        setUpInterceptor(ENABLED)

        val delay = measureTimeMillis {
            executeGetRequest("/delay").body()?.string()
        }

        val noDelay = measureTimeMillis {
            executeGetRequest("/request").body()?.string()
        }

        val threshold = 50
        assertTrue(delay > threshold, "Time was $delay (< $threshold ms)")
        assertTrue(noDelay < threshold, "Time without delay was $noDelay (> $threshold ms)")
    }

    @Test
    fun `should delegate path resolutions`() {
        setUpInterceptor(ENABLED)

        val request = initRequest("/request")
        client.newCall(request).execute()

        verify(filingPolicy).getPath(request)
    }

    @Test
    fun `should support mixed mode to execute request when no response is found locally`() {
        enqueueServerResponse(200, "body")
        setUpInterceptor(MIXED)

        val serverResponse = executeGetRequest("")
        val localResponse = executeGetRequest("/request")

        assertResponseCode(serverResponse, 200, "OK")
        assertEquals("body", serverResponse.body()?.string())
        assertResponseCode(localResponse, 200, "OK")
        assertEquals("simple body", localResponse.body()?.string())
    }

    @Test
    fun `should let requests through when recording`() {
        enqueueServerResponse(200, "body")
        setUpInterceptor(RECORD, SAVE_FOLDER)

        val response = executeGetRequest("record/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @Test
    fun `should let requests through when recording even if saving fails`() {
        enqueueServerResponse(200, "body")
        setUpInterceptor(RECORD, "")

        val response = executeGetRequest("record/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @Test
    fun `should store requests and responses in the proper locations when recording`() {
        enqueueServerResponse(200, "body")
        setUpInterceptor(RECORD, SAVE_FOLDER)

        executeGetRequest("record/request")

        assertFileExists("$SAVE_FOLDER/record/request.json")
        assertFileExists("$SAVE_FOLDER/record/request_body_0.txt")
    }

    @Test
    fun `should store requests and responses when recording`() {
        enqueueServerResponse(200, "body", listOf("someKey" to "someValue"))
        setUpInterceptor(RECORD, SAVE_FOLDER)

        executeRequest("request?param1=value1", "POST", "requestBody", listOf("someHeader" to "someValue"))

        withFile("$SAVE_FOLDER/request.json") {
            val result: List<Matcher> =
                jacksonObjectMapper().readValue(it, jacksonTypeRef<List<Matcher>>())
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

    @Test
    fun `should update existing descriptors when recording`() {
        enqueueServerResponse(200, "body", listOf("someKey" to "someValue"))
        enqueueServerResponse(200, "second body")
        setUpInterceptor(RECORD, SAVE_FOLDER)

        executeRequest("request?param1=value1", "POST", "requestBody", listOf("someHeader" to "someValue"))
        executeGetRequest("request")

        withFile("$SAVE_FOLDER/request.json") {
            val result: List<Matcher> =
                jacksonObjectMapper().readValue(it, jacksonTypeRef<List<Matcher>>())
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
                        headers = listOf(Header("Content-Length", "11"), Header("Content-Type", "text/plain"))
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

    @Test
    fun `should add proper extension to response files`() {
        enqueueServerResponse(200, "body", contentType = "image/png")
        enqueueServerResponse(200, "body", contentType = "application/json")
        setUpInterceptor(RECORD, SAVE_FOLDER)

        executeGetRequest("record/request1")
        executeGetRequest("record/request2")

        assertFileExists("$SAVE_FOLDER/record/request1_body_0.png")
        assertFileExists("$SAVE_FOLDER/record/request2_body_0.json")
    }

    @Test
    fun `should match indexes in descriptor file and actual response file name`() {
        enqueueServerResponse(200, "body", contentType = "image/png")
        enqueueServerResponse(200, "body", contentType = "application/json")
        setUpInterceptor(RECORD, SAVE_FOLDER)

        executeGetRequest("record/request")
        executeGetRequest("record/request")

        assertFileExists("$SAVE_FOLDER/record/request_body_0.png")
        assertFileExists("$SAVE_FOLDER/record/request_body_1.json")
    }

    @Test
    fun `should not allow to record requests if root folder is not set`() {

        try {
            setUpInterceptor(RECORD)
            fail("Should not allow to record if root folder was not provided")
        } catch (e: IllegalStateException) {
            assertEquals(DISABLED, interceptor.mode)
        }
    }

    @Test
    fun `should allow to stack several interceptors thanks to mixed mode`() {
        enqueueServerResponse(200, "server response")

        val policy = InMemoryPolicy()
        policy.addMatcher(
            "$mockServerBaseUrl/inMemory", Matcher(
                RequestDescriptor(method = "GET"),
                ResponseDescriptor(
                    code = 200,
                    body = "in memory response",
                    mediaType = "text/plain"
                )
            )
        )
        val inMemoryInterceptor = MockResponseInterceptor(policy, policy::matchRequest)
        inMemoryInterceptor.mode = MIXED

        val fileBasedInterceptor = MockResponseInterceptor(filingPolicy, loadingLambda)
        fileBasedInterceptor.mode = MIXED

        client = OkHttpClient.Builder()
            .addInterceptor(inMemoryInterceptor)
            .addInterceptor(fileBasedInterceptor)
            .build()

        assertEquals("in memory response", executeGetRequest("inMemory").body()?.string())
        assertEquals("file response", executeGetRequest("fileMatch").body()?.string())
        assertEquals("server response", executeGetRequest("serverMatch").body()?.string())
    }

    // TODO null response body?

    private fun File.readAsString() = FileInputStream(this).readAsString()

    private fun InputStream.readAsString(): String = bufferedReader().use { reader -> reader.readText() }

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
                .forEach {
                    it.delete()
                }
        }
    }

    private fun setUpInterceptor(
        mode: MockResponseInterceptor.MODE,
        rootFolder: String? = null
    ) {
        interceptor = MockResponseInterceptor(filingPolicy, loadingLambda, rootFolder?.let { File(it) })
        interceptor.mode = mode
        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }


    private fun assertResponseCode(response: Response, code: Int, message: String) {
        assertEquals(code, response.code())
        assertEquals(message, response.message())
    }

    private fun enqueueServerResponse(
        responseCode: Int,
        responseBody: String,
        headers: List<Pair<String, String>> = listOf(),
        contentType: String? = null
    ) {
        val serverResponse = MockResponse().apply {
            setResponseCode(responseCode)
            setBody(responseBody)
            addHeader("Content-Type", contentType ?: "text/plain")
            headers.forEach { addHeader(it.first, it.second) }
        }
        server.enqueue(serverResponse)
    }

    private fun executeGetRequest(url: String, headers: List<Pair<String, String>> = emptyList()): Response =
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

    }
}