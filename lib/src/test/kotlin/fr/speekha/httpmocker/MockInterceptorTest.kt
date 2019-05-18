package fr.speekha.httpmocker

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MockInterceptorTest {

    private val server = MockWebServer()

    private val mockServerBaseUrl: String
        get() = "http://127.0.0.1:${server.port}"

    private val interceptor = MockResponseInterceptor()

    private val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    @Before
    fun setUp() {
        server.start()
    }

    @Test
    fun `should not interfere with requests when disabled`() {
        enqueueServerResponse(200, "body")

        val response = testRequest(mockServerBaseUrl)

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @Test
    fun `should return a 404 error when response is not found`() {
        interceptor.enabled = true

        val response = testRequest("$mockServerBaseUrl/unknown")

        assertResponseCode(response, 404, "Not Found")
    }

    @Test
    fun `should return a 200 when response is found`() {
        interceptor.enabled = true

        val response = testRequest("$mockServerBaseUrl/request")

        assertResponseCode(response, 200, "OK")
    }

    @Test
    fun `should return a predefined response body from file`() {
        interceptor.enabled = true

        val response = testRequest("$mockServerBaseUrl/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple body", response.body()?.string())
    }

    @Test
    fun `should return proper headers`() {
        interceptor.enabled = true

        val response = testRequest("$mockServerBaseUrl/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("simple header", response.header("testHeader"))
    }

    @Test
    fun `should handle redirects`() {
        interceptor.enabled = true

        val response = testRequest("$mockServerBaseUrl/redirect")

        assertResponseCode(response, 302, "Found")
        assertEquals("http://www.google.com", response.header("Location"))
    }

    @Test
    fun `should handle media type`() {
        interceptor.enabled = true

        val response = testRequest("$mockServerBaseUrl/mediatype")

        assertResponseCode(response, 200, "OK")
        assertEquals("application", response.body()?.contentType()?.type())
        assertEquals("json", response.body()?.contentType()?.subtype())
    }

    @Test
    fun `should select response based on query params`() {
        interceptor.enabled = true

        val param1 = testRequest("$mockServerBaseUrl/query_param?param=1").body()?.string()
        val param2 = testRequest("$mockServerBaseUrl/query_param?param=2").body()?.string()

        assertEquals("param A", param1)
        assertEquals("param B", param2)
    }

    @Test
    fun `should select response based on headers`() {
        interceptor.enabled = true

        val param1 = testRequest("$mockServerBaseUrl/headers").body()?.string()
        val param2 = testRequest("$mockServerBaseUrl/headers", listOf("header1" to "1", "header2" to "2")).body()?.string()

        assertEquals("no header", param1)
        assertEquals("with headers", param2)
    }

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

    private fun testRequest(url: String, headers: List<Pair<String, String>> = emptyList()): Response {
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(*headers.flatMap { listOf(it.first, it.second) }.toTypedArray()))
            .build()
        return client.newCall(request).execute()
    }


}