package fr.speekha.httpmocker

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import okhttp3.*
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
        on { invoke(any()) } doAnswer {  javaClass.classLoader.getResourceAsStream(it.getArgument(0)) }
    }

    private val interceptor = MockResponseInterceptor {
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

        val response = getRequest("")

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @Test
    fun `should return a 404 error when response is not found`() {
        interceptor.enabled = true

        val response = getRequest("/unknown")

        assertResponseCode(response, 404, "Not Found")
    }

    @Test
    fun `should return a 404 error when an exception occurs`() {

        whenever(loadingLambda.invoke(any())) doAnswer {
            error("Loading error")
        }
        interceptor.enabled = true

        val response = getRequest("/unknown")

        assertResponseCode(response, 404, "Not Found")
    }

    @Test
    fun `should return a 200 when response is found`() {
        interceptor.enabled = true

        val response = getRequest("/request")

        assertResponseCode(response, 200, "OK")
    }

    @Test
    fun `should return a predefined response body from json descriptor`() {
        interceptor.enabled = true

        val response = getRequest("/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple body", response.body()?.string())
    }

    @Test
    fun `should return a predefined response body from separate file`() {
        interceptor.enabled = true

        val response = getRequest("/body_file")

        assertResponseCode(response, 200, "OK")
        assertEquals("separate body file", response.body()?.string())
    }

    @Test
    fun `should return proper headers`() {
        interceptor.enabled = true

        val response = getRequest("/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple header", response.header("testHeader"))
    }

    @Test
    fun `should handle redirects`() {
        interceptor.enabled = true

        val response = getRequest("/redirect")

        assertResponseCode(response, 302, "Found")
        assertEquals("http://www.google.com", response.header("Location"))
    }

    @Test
    fun `should handle media type`() {
        interceptor.enabled = true

        val response = getRequest("/mediatype")

        assertResponseCode(response, 200, "OK")
        assertEquals("application", response.body()?.contentType()?.type())
        assertEquals("json", response.body()?.contentType()?.subtype())
    }

    @Test
    fun `should select response based on query params`() {
        interceptor.enabled = true

        val param1 = getRequest("/query_param?param=1").body()?.string()
        val param2 = getRequest("/query_param?param=2").body()?.string()

        assertEquals("param A", param1)
        assertEquals("param B", param2)
    }

    @Test
    fun `should select response based on headers`() {
        interceptor.enabled = true

        val param1 = getRequest("/headers").body()?.string()
        val param2 = getRequest(
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
        interceptor.enabled = true

        val get = getRequest("/method").body()?.string()
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
        interceptor.enabled = true

        val match = executeRequest("/body_matching", "POST", "azer1zere").body()?.string()
        val noMatch = executeRequest("/body_matching", "POST", "azerzere").body()?.string()

        assertEquals("matched", match)
        assertEquals("no match", noMatch)
    }

    @Test
    fun `should allow to delay all responses`() {
        interceptor.enabled = true
        interceptor.delay = 50

        val delay = measureTimeMillis {
            getRequest("/request").body()?.string()
        }

        val threshold = 50
        assertTrue(delay > threshold, "Time was $delay (< $threshold ms)")
    }

    @Test
    fun `should allow to delay responses based on configuration`() {
        interceptor.enabled = true

        val delay = measureTimeMillis {
            getRequest("/delay").body()?.string()
        }

        val noDelay = measureTimeMillis {
            getRequest("/request").body()?.string()
        }

        val threshold = 50
        assertTrue(delay > threshold, "Time was $delay (< $threshold ms)")
        assertTrue(noDelay < threshold, "Time without delay was $noDelay (> $threshold ms)")
    }

    // TODO filing policy

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

    private fun getRequest(url: String, headers: List<Pair<String, String>> = emptyList()): Response =
        executeRequest(url, "GET", null, headers)

    private fun executeRequest(
        url: String,
        method: String,
        body: String?,
        headers: List<Pair<String, String>> = emptyList()
    ): Response {
        val path = if (url.startsWith("/")) url.drop(1) else url
        val request = Request.Builder()
            .url("$mockServerBaseUrl/$path")
            .headers(Headers.of(*headers.flatMap { listOf(it.first, it.second) }.toTypedArray()))
            .method(method, body?.let { RequestBody.create(MediaType.parse("text/plain"), it) })
            .build()
        return client.newCall(request).execute()
    }

}