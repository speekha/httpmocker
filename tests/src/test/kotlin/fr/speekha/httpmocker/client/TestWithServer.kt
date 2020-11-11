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

import fr.speekha.httpmocker.custom.CustomMapper
import fr.speekha.httpmocker.gson.GsonMapper
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.kotlinx.KotlinxMapper
import fr.speekha.httpmocker.moshi.MoshiMapper
import fr.speekha.httpmocker.sax.SaxMapper
import fr.speekha.httpmocker.serialization.JsonFormatConverter
import fr.speekha.httpmocker.withFile
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

open class TestWithServer {

    protected val server = MockWebServer()

    private val mockServerBaseUrl: String
        get() = "http://127.0.0.1:${server.port}"

    @Before
    fun setUp() {
        server.start()
    }

    fun enqueueServerResponseTmp(
        responseCode: Int,
        responseBody: String?,
        headers: List<Pair<String, String>>,
        contentType: String?
    ) = enqueueServerResponse(responseCode, responseBody, headers, contentType)

    protected fun enqueueServerResponse(
        responseCode: Int,
        responseBody: String?,
        headers: List<Pair<String, String>> = emptyList(),
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

    private fun assertFileExists(path: String) = withFile(path) {
        assertTrue(it.exists(), "File $path does not exist")
    }

    fun assertFilesExist(vararg path: String) = path.forEach { assertFileExists(it) }

    fun completeLocalUrl(url: String): String = if (url.startsWith("http")) {
        url
    } else {
        val path = if (url.startsWith("/")) url.drop(1) else url
        "$mockServerBaseUrl/$path"
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun mappers(): Stream<Arguments> = Stream.concat(jsonMappers(), xmlMappers())

        @JvmStatic
        @Suppress("unused")
        fun jsonMappers(): Stream<Arguments> = Stream.of(
            Arguments.of("Jackson", JacksonMapper(), "json"),
            Arguments.of("Gson", GsonMapper(), "json"),
            Arguments.of("Moshi", MoshiMapper(), "json"),
            Arguments.of("Custom mapper", CustomMapper(), "json"),
            Arguments.of(
                "Kotlinx serialization",
                KotlinxMapper(JsonFormatConverter()::expand, JsonFormatConverter()::compact),
                "json"
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun xmlMappers(): Stream<Arguments> = Stream.of(
            Arguments.of("XML mapper", SaxMapper(), "xml")
        )

        const val REQUEST_OK_CODE = 200
        const val REQUEST_OK_MESSAGE = "OK"
        const val NOT_FOUND_CODE = 404
        const val URL_SIMPLE_REQUEST = "/request"
        const val URL_HEADERS = "/headers"
        const val URL_METHOD = "/method"
        const val REQUEST_SIMPLE_BODY = "simple body"
    }
}
