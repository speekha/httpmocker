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

import fr.speekha.httpmocker.JsonFormatConverter
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.buildRequest
import fr.speekha.httpmocker.custom.CustomMapper
import fr.speekha.httpmocker.gson.GsonMapper
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.kotlinx.KotlinxMapper
import fr.speekha.httpmocker.moshi.MoshiMapper
import fr.speekha.httpmocker.readAsString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.provider.Arguments
import java.io.File
import java.io.FileInputStream
import java.util.stream.Stream

open class TestWithServer {

    protected val server = MockWebServer()

    protected val mockServerBaseUrl: String
        get() = "http://127.0.0.1:${server.port}"

    protected lateinit var interceptor: MockResponseInterceptor

    protected lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
    }

    protected fun enqueueServerResponse(
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

    protected fun File.readAsString() = FileInputStream(this).readAsString()

    protected fun assertFileExists(path: String) = withFile(path) {
        assertTrue(it.exists(), "File $path does not exist")
    }

    protected fun <T : Any?> withFile(path: String, block: (File) -> T) = block(File(path))

    protected fun assertResponseCode(response: Response, code: Int, message: String) {
        Assertions.assertEquals(code, response.code())
        Assertions.assertEquals(message, response.message())
    }

    protected fun executeGetRequest(
        url: String,
        headers: List<Pair<String, String>> = emptyList()
    ): Response =
        executeRequest(url, "GET", null, headers)

    protected fun executeRequest(
        url: String,
        method: String,
        body: String?,
        headers: List<Pair<String, String>> = emptyList()
    ): Response {
        val request = initRequest(url, headers, method, body)
        return client.newCall(request).execute()
    }

    protected fun initRequest(
        url: String,
        headers: List<Pair<String, String>> = emptyList(),
        method: String = "GET",
        body: String? = null
    ): Request {
        val path = if (url.startsWith("/")) url.drop(1) else url
        return buildRequest(
            "$mockServerBaseUrl/$path",
            headers,
            method,
            body
        )
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun mappers(): Stream<Arguments> = Stream.of(
            Arguments.of("Jackson", JacksonMapper()),
            Arguments.of("Gson", GsonMapper()),
            Arguments.of("Moshi", MoshiMapper()),
            Arguments.of("Custom mapper", CustomMapper()),
            Arguments.of(
                "Kotlinx serialization",
                KotlinxMapper(JsonFormatConverter()::expand, JsonFormatConverter()::compact)
            )
        )

        const val REQUEST_OK_CODE = 200
        const val REQUEST_OK_MESSAGE = "OK"
        const val NOT_FOUND_CODE = 404
        const val NOT_FOUND_MESSAGE = "Not Found"
    }
}
