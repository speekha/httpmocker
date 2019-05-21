package fr.speekha.httpmocker

import com.nhaarman.mockitokotlin2.*
import fr.speekha.httpmocker.MockResponseInterceptor.MODE.ENABLED
import fr.speekha.httpmocker.MockResponseInterceptor.MODE.MIXED
import fr.speekha.httpmocker.policies.FilingPolicy
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream
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

    private val interceptor = MockResponseInterceptor(filingPolicy) {
        loadingLambda(it)
    }

    private val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    @Before
    fun setUp() {
        server.start()
    }

    @Test
    fun `should not interfere with requests when disabled`() {
        enqueueServerResponse(200, "body")

        val response = executeGetRequest("")

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @Test
    fun `should return a 404 error when response is not found`() {
        interceptor.mode = ENABLED

        val response = executeGetRequest("/unknown")

        assertResponseCode(response, 404, "Not Found")
    }

    @Test
    fun `should return a 404 error when an exception occurs`() {
        whenever(loadingLambda.invoke(any())) doAnswer {
            error("Loading error")
        }
        interceptor.mode = ENABLED

        val response = executeGetRequest("/unknown")

        assertResponseCode(response, 404, "Not Found")
    }

    @Test
    fun `should return a 404 error when no response matches the criteria`() {
        whenever(loadingLambda.invoke(any())) doAnswer {
            error("Loading error")
        }
        interceptor.mode = ENABLED

        val response = executeGetRequest("/no_match")

        assertResponseCode(response, 404, "Not Found")
    }

    @Test
    fun `should return a 200 when response is found`() {
        interceptor.mode = ENABLED

        val response = executeGetRequest("/request")

        assertResponseCode(response, 200, "OK")
    }

    @Test
    fun `should return a predefined response body from json descriptor`() {
        interceptor.mode = ENABLED

        val response = executeGetRequest("/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple body", response.body()?.string())
    }

    @Test
    fun `should return a predefined response body from separate file`() {
        interceptor.mode = ENABLED

        val response = executeGetRequest("/body_file")

        assertResponseCode(response, 200, "OK")
        assertEquals("separate body file", response.body()?.string())
    }

    @Test
    fun `should return a predefined response body from separate file in the same folder`() {
        interceptor.mode = ENABLED

        val response = executeGetRequest("/folder/request_in_folder")

        assertResponseCode(response, 200, "OK")
        assertEquals("separate body file", response.body()?.string())
    }

    @Test
    fun `should return proper headers`() {
        interceptor.mode = ENABLED

        val response = executeGetRequest("/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple header", response.header("testHeader"))
    }

    @Test
    fun `should handle redirects`() {
        interceptor.mode = ENABLED

        val response = executeGetRequest("/redirect")

        assertResponseCode(response, 302, "Found")
        assertEquals("http://www.google.com", response.header("Location"))
    }

    @Test
    fun `should handle media type`() {
        interceptor.mode = ENABLED

        val response = executeGetRequest("/mediatype")

        assertResponseCode(response, 200, "OK")
        assertEquals("application", response.body()?.contentType()?.type())
        assertEquals("json", response.body()?.contentType()?.subtype())
    }

    @Test
    fun `should select response based on query params`() {
        interceptor.mode = ENABLED

        val param1 = executeGetRequest("/query_param?param=1").body()?.string()
        val param2 = executeGetRequest("/query_param?param=2").body()?.string()

        assertEquals("param A", param1)
        assertEquals("param B", param2)
    }

    @Test
    fun `should select response based on headers`() {
        interceptor.mode = ENABLED

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
        interceptor.mode = ENABLED

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
        interceptor.mode = ENABLED

        val match = executeRequest("/body_matching", "POST", "azer1zere").body()?.string()
        val noMatch = executeRequest("/body_matching", "POST", "azerzere").body()?.string()

        assertEquals("matched", match)
        assertEquals("no match", noMatch)
    }

    @Test
    fun `should allow to delay all responses`() {
        interceptor.mode = ENABLED
        interceptor.delay = 50

        val delay = measureTimeMillis {
            executeGetRequest("/request").body()?.string()
        }

        val threshold = 50
        assertTrue(delay > threshold, "Time was $delay (< $threshold ms)")
    }

    @Test
    fun `should allow to delay responses based on configuration`() {
        interceptor.mode = ENABLED

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
        interceptor.mode = ENABLED

        val request = initRequest("/request")
        client.newCall(request).execute()

        verify(filingPolicy).getPath(request)
    }

    @Test
    fun `should support mixed mode to execute request when no response is found locally`() {
        enqueueServerResponse(200, "body")
        interceptor.mode = MIXED

        val serverResponse = executeGetRequest("")
        val localResponse = executeGetRequest("/request")

        assertResponseCode(serverResponse, 200, "OK")
        assertEquals("body", serverResponse.body()?.string())
        assertResponseCode(localResponse, 200, "OK")
        assertEquals("simple body", localResponse.body()?.string())

    }

    // TODO recording requests

    private fun assertResponseCode(response: Response, code: Int, message: String) {
        assertEquals(code, response.code())
        assertEquals(message, response.message())
    }

    private fun enqueueServerResponse(responseCode: Int, responseBody: String) {
        val serverResponse = MockResponse().apply {
            setResponseCode(responseCode)
            setBody(responseBody)
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
}