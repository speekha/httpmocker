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

import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.MockResponseInterceptor.Mode.RECORD
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.stream.Stream

class RecordTests : TestWithServer() {

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should let requests through when recording`(title: String, mapper: Mapper) {
        enqueueServerResponse(200, "body")
        setUpInterceptor(mapper)

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
        setUpInterceptor(mapper, "", false)

        val response = executeGetRequest("record/request")

        assertResponseCode(response, 200, "OK")
        assertEquals("body", response.body()?.string())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `recording failure should return an error if desired`(
        title: String,
        mapper: Mapper
    ) {
        enqueueServerResponse(200, "body")
        setUpInterceptor(mapper, "", true)

        assertThrows<FileNotFoundException> {
            executeGetRequest("record/request")
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should store requests and responses in the proper locations when recording`(
        title: String,
        mapper: Mapper
    ) {
        enqueueServerResponse(200, "body")
        setUpInterceptor(mapper)

        executeGetRequest("record/request")

        assertFileExists("$SAVE_FOLDER/record/request.json")
        assertFileExists("$SAVE_FOLDER/record/request_body_0.txt")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should name body file correctly when last path segment is empty`(
        title: String,
        mapper: Mapper
    ) {
        enqueueServerResponse(200, "body")
        setUpInterceptor(mapper)

        executeGetRequest("record/")

        assertFileExists("$SAVE_FOLDER/record/index.json")
        assertFileExists("$SAVE_FOLDER/record/index_body_0.txt")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `should store requests and responses when recording`(title: String, mapper: Mapper) {
        enqueueServerResponse(200, "body", listOf("someKey" to "someValue"))
        setUpInterceptor(mapper)

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
        setUpInterceptor(mapper)

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
        setUpInterceptor(mapper)

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
        setUpInterceptor(mapper)

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
        setUpInterceptor(mapper)

        executeGetRequest("record/request")
        executeGetRequest("record/request")

        assertFileExists("$SAVE_FOLDER/record/request_body_0.png")
        assertFileExists("$SAVE_FOLDER/record/request_body_1.json")
    }

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
        mapper: Mapper,
        rootFolder: String = SAVE_FOLDER,
        failOnError: Boolean = false
    ) {
        interceptor = MockResponseInterceptor.Builder()
            .decodeScenarioPathWith {
                val path = it.url().encodedPath()
                (path + if (path.endsWith("/")) "index.json" else ".json")
                    .drop(1)
            }
            .parseScenariosWith(mapper)
            .saveScenariosIn(File(rootFolder))
            .failOnRecordingError(failOnError)
            .setInterceptorStatus(RECORD)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    companion object {
        private const val SAVE_FOLDER = "testFolder"

        @JvmStatic
        fun data(): Stream<Arguments> =
            mappers
    }
}