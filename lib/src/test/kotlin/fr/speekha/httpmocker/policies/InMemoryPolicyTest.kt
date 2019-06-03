package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.MockResponseInterceptor.MODE.ENABLED
import fr.speekha.httpmocker.buildRequest
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InMemoryPolicyTest {
    @Test
    fun `should return URL as path`() {
        val policy = InMemoryPolicy()
        val url = "http://www.test.fr/path?param=1"
        assertEquals(url, policy.getPath(buildRequest(url)))
    }

    @Test
    fun `should allow to retrieve a scenario based on a URL`() {
        val url = "http://www.test.fr/path1?param=1"
        val policy = InMemoryPolicy()
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

        val interceptor = MockResponseInterceptor(policy, policy::matchRequest)
        interceptor.mode = ENABLED
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val getResponse = client.newCall(buildRequest(url, listOf(), "GET")).execute()

        assertEquals(200, getResponse.code())
        assertEquals("get some body", getResponse.body()?.string())
    }

    @Test
    fun `should allow to add several matchers for the same URL`() {
        val url = "http://www.test.fr/path1?param=1"
        val policy = InMemoryPolicy()
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

        val interceptor = MockResponseInterceptor(policy, policy::matchRequest)
        interceptor.mode = ENABLED
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val getResponse = client.newCall(buildRequest(url, listOf(), "GET")).execute()
        val postResponse = client.newCall(buildRequest(url, listOf(), "POST", "body")).execute()

        assertEquals("get some body", getResponse.body()?.string())
        assertEquals("post some body", postResponse.body()?.string())
    }

    @Test
    fun `should allow to add matchers for different URLs`() {
        val url1 = "http://www.test.fr/path1?param=1"
        val url2 = "http://www.test.fr/path2?param=1"
        val policy = InMemoryPolicy()
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

        val interceptor = MockResponseInterceptor(policy, policy::matchRequest)
        interceptor.mode = ENABLED
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val response1 = client.newCall(buildRequest(url1, listOf(), "GET")).execute()
        val response2 = client.newCall(buildRequest(url2, listOf(), "GET")).execute()

        assertEquals("first body", response1.body()?.string())
        assertEquals("second body", response2.body()?.string())
    }
}