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
import fr.speekha.httpmocker.NO_RECORDER_ERROR
import fr.speekha.httpmocker.NO_ROOT_FOLDER_ERROR
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.readMatches
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections

@Suppress("UNUSED_PARAMETER")
class RecordTests : TestWithServer() {

    @Nested
    @DisplayName("Given an mock interceptor with no recorder set")
    inner class NoRecorderSet {
        private lateinit var interceptor: MockResponseInterceptor

        @Test
        @DisplayName("When building the interceptor in record mode, then an error should occur")
        fun `should not allow to init an interceptor in record mode with no recorder`() {
            val exception = assertThrows<IllegalStateException> { setupProvider(RECORD) }
            assertEquals(NO_ROOT_FOLDER_ERROR, exception.message)
            Assertions.assertFalse(::interceptor.isInitialized)
        }

        @Test
        @DisplayName("When setting the interceptor status to record mode, then an error should occur")
        fun `should not allow to record requests if recorder is not set`() {
            setupProvider()
            val exception = assertThrows<IllegalStateException> {
                interceptor.mode = RECORD
            }
            assertEquals(NO_RECORDER_ERROR, exception.message)
        }

        private fun setupProvider(
            status: MockResponseInterceptor.Mode = MockResponseInterceptor.Mode.ENABLED
        ) {
            interceptor = MockResponseInterceptor.Builder()
                .useDynamicMocks { null }
                .setInterceptorStatus(status)
                .build()

            client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        }
    }

    @Nested
    @DisplayName("Given an mock interceptor in record mode")
    inner class InterceptionTest {
        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is recorded, then it should not be blocked")
        fun `should let requests through when recording`(title: String, mapper: Mapper) {
            enqueueServerResponse(200, "body")
            setUpInterceptor(mapper)

            val response = executeGetRequest("record/request")

            assertResponseCode(response, 200, "OK")
            assertEquals("body", response.body()?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When recording a request fails, then it should not interfere with the request")
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

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a request fails and errors are expected, " +
                    "then the error should be returned"
        )
        fun `recording failure should return an error if desired`(title: String, mapper: Mapper) {
            enqueueServerResponse(200, "body")
            setUpInterceptor(mapper, "", true)

            assertThrows<FileNotFoundException> {
                executeGetRequest("record/request")
            }
        }
    }

    @Nested
    @DisplayName("Given an mock interceptor in record mode with a root folder")
    inner class RecordTest {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a request, " +
                    "then scenario and response body files should be created in that folder"
        )
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

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a request for a URL ending with a '/', " +
                    "then scenario files should be named with 'index'"
        )
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

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When recording a request, then content of scenario files should be correct")
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
                val result = mapper.readMatches(it)
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

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a request or response with a null body, " +
                    "then body should be empty in scenario files"
        )
        fun `should handle null request and response bodies when recording`(
            title: String,
            mapper: Mapper
        ) {
            enqueueServerResponse(200, null)
            setUpInterceptor(mapper)

            executeRequest("request", "GET", null)

            withFile("$SAVE_FOLDER/request.json") {
                val result = mapper.readMatches(it)
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

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When a scenario already exists for a request, " +
                    "then the scenario should be completed with the new one"
        )
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
                val result = mapper.readMatches(it)
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

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a response body, " +
                    "then the file should have the proper extension"
        )
        fun `should add proper extension to response files`(title: String, mapper: Mapper) {
            enqueueServerResponse(200, "body", contentType = "image/png")
            enqueueServerResponse(200, "body", contentType = "application/json")
            setUpInterceptor(mapper)

            executeGetRequest("record/request1")
            executeGetRequest("record/request2")

            assertFileExists("$SAVE_FOLDER/record/request1_body_0.png")
            assertFileExists("$SAVE_FOLDER/record/request2_body_0.json")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When several matches exist for a request, " +
                    "then the body file should have the same index as the request in the scenario"
        )
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

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a request fails with an exception, " +
                    "then the exception should be recorded"
        )
        fun `recording failure should save error in scenario`(title: String, mapper: Mapper) {
            setUpInterceptor(mapper)

            val exception = assertThrows<java.net.UnknownHostException> {
                val request = Request.Builder().url("http://falseUrl.wrong/record/error").build()
                client.newCall(request).execute()
            }

            assertFileExists("$SAVE_FOLDER/record/error.json")
            withFile("$SAVE_FOLDER/record/error.json") {
                val result = mapper.readMatches(it)
                val expectedResult = Matcher(
                    request = RequestDescriptor(
                        method = "GET"
                    ),
                    error = NetworkError(
                        exceptionType = exception.javaClass.canonicalName,
                        message = exception.message
                    )
                )
                assertEquals(listOf(expectedResult), result)
            }
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
    }
}
