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

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.Mode.ENABLED
import fr.speekha.httpmocker.Mode.RECORD
import fr.speekha.httpmocker.NO_RECORDER_ERROR
import fr.speekha.httpmocker.NO_ROOT_FOLDER_ERROR
import fr.speekha.httpmocker.builder.mockInterceptor
import fr.speekha.httpmocker.builder.recordScenariosIn
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.MirrorPathPolicy
import fr.speekha.httpmocker.serialization.Mapper
import fr.speekha.httpmocker.serialization.readMatches
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
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList
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
            status: Mode = ENABLED
        ) {
            interceptor = mockInterceptor {
                useDynamicMocks { null }
                setInterceptorStatus(status)
            }

            client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        }
    }

    @Nested
    @DisplayName("Given an mock interceptor")
    inner class PolicyTest : RecorderTestSuite {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a recording policy is set, then it should be used")
        fun `should use recording policy`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            val policy: FilingPolicy = mock {
                on { getPath(any()) } doReturn "record_policy.$fileType"
            }

            testInterceptor {
                mockInterceptor {
                    decodeScenarioPathWith {
                        "wrongPolicy.$fileType"
                    }
                    parseScenariosWith(mapper)
                    recordScenariosIn(SAVE_FOLDER) with policy
                    failOnRecordingError(true)
                    setInterceptorStatus(RECORD)
                }
            }
            assertFilesExist("$SAVE_FOLDER/record_policy.$fileType", requestBodyFile)
            verify(policy).getPath(any())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a recording policy is set as lambda, then it should be used")
        fun `should use recording policy as lambda`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            testInterceptor {
                mockInterceptor {
                    decodeScenarioPathWith {
                        "wrongPolicy.$fileType"
                    }
                    parseScenariosWith(mapper)
                    recordScenariosIn(SAVE_FOLDER) with { "lambda_policy.$fileType" }
                    failOnRecordingError(true)
                    setInterceptorStatus(RECORD)
                }
            }
            assertFilesExist("$SAVE_FOLDER/lambda_policy.$fileType", requestBodyFile)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When no recording policy is set, then the read policy should be used")
        fun `should use read policy`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            val policy: FilingPolicy = mock {
                on { getPath(any()) } doReturn "read_policy.$fileType"
            }
            testInterceptor {
                mockInterceptor {
                    decodeScenarioPathWith(policy)
                    parseScenariosWith(mapper)
                    recordScenariosIn(SAVE_FOLDER)
                    failOnRecordingError(true)
                    setInterceptorStatus(RECORD)
                }
            }
            assertFilesExist("$SAVE_FOLDER/read_policy.$fileType", requestBodyFile)
            verify(policy).getPath(any())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When no policy is set at all, then a default Mirror path policy should be used")
        fun `should use default policy`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            testInterceptor {
                mockInterceptor {
                    parseScenariosWith(mapper)
                    recordScenariosIn(SAVE_FOLDER)
                    failOnRecordingError(true)
                    setInterceptorStatus(RECORD)
                }
            }
            assertFilesExist("$SAVE_FOLDER/request.$fileType", requestBodyFile)
        }

        private fun testInterceptor(buildInterceptor: () -> MockResponseInterceptor) {
            enqueueServerResponse(200, "body", ArrayList(), null)
            interceptor = buildInterceptor()
            client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            executeGetRequest(requestUrl)
        }

        private val requestBodyFile = "$SAVE_FOLDER/request_body_0.txt"
    }

    @Nested
    @DisplayName("Given an mock interceptor in record mode")
    inner class InterceptionTest : RecorderTestSuite {
        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When a request is recorded, then it should not be blocked")
        fun `should let requests through when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body")
            setUpInterceptor(mapper, fileType = fileType)

            val response = executeGetRequest(recordRequestUrl)

            assertResponseCode(response, 200, "OK")
            assertEquals("body", response.body()?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When recording a request fails, then it should not interfere with the request")
        fun `should let requests through when recording even if saving fails`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body")
            setUpInterceptor(mapper, "", false, fileType)

            val response = executeGetRequest(recordRequestUrl)

            assertResponseCode(response, 200, "OK")
            assertEquals("body", response.body()?.string())
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a request fails and errors are expected, " +
                "then the error should be returned"
        )
        fun `recording failure should return an error if desired`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body")
            setUpInterceptor(mapper, "", true, fileType)

            assertThrows<FileNotFoundException> {
                executeGetRequest(recordRequestUrl)
            }
        }
    }

    @Nested
    @DisplayName("Given an mock interceptor in record mode with a root folder")
    inner class RecordTest : RecorderTestSuite {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a request, " +
                "then scenario and response body files should be created in that folder"
        )
        fun `should store requests and responses in the proper locations when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body")
            setUpInterceptor(mapper, fileType = fileType)

            executeGetRequest(recordRequestUrl)

            assertFileExists("$SAVE_FOLDER/record/request.$fileType")
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
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body")
            setUpInterceptor(mapper, fileType = fileType)

            executeGetRequest("record/")

            assertFileExists("$SAVE_FOLDER/record/index.$fileType")
            assertFileExists("$SAVE_FOLDER/record/index_body_0.txt")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName("When recording a request, then content of scenario files should be correct")
        fun `should store requests and responses when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body", listOf("someKey" to "someValue"))
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest(
                "request?param1=value1",
                "POST",
                "requestBody",
                listOf("someHeader" to "someValue")
            )

            withFile(fileName(requestUrl, fileType)) {
                val result = mapper.readMatches(it)
                val expectedResult = requestWithParams()
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
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, null)
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest(requestUrl, "GET", null)

            withFile(fileName(requestUrl, fileType)) {
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
        fun `should update existing descriptors when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body", listOf("someKey" to "someValue"))
            enqueueServerResponse(200, "second body")
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest(
                "request?param1=value1",
                "POST",
                "requestBody",
                listOf("someHeader" to "someValue")
            )
            executeGetRequest(requestUrl)

            withFile(fileName(requestUrl, fileType)) {
                val result = mapper.readMatches(it)
                val expectedResult = listOf(
                    requestWithParams(),
                    requestWithNoParams()
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
        fun `should add proper extension to response files`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body", contentType = "image/png")
            enqueueServerResponse(200, "body", contentType = "application/json")
            setUpInterceptor(mapper, fileType = fileType)

            executeGetRequest("record/request1")
            executeGetRequest("record/request2")

            assertFileExists("$SAVE_FOLDER/record/request1_body_0.png")
            assertFileExists("$SAVE_FOLDER/record/request2_body_0.json")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a response body with a mediatype charset, " +
                "then the file should have the proper extension"
        )
        fun `should handle proper extension for response files`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body", contentType = "application/json; charset=UTF-8")
            setUpInterceptor(mapper, fileType = fileType)

            executeGetRequest("record/request1")

            assertFileExists("$SAVE_FOLDER/record/request1_body_0.json")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a response body with unknwown mediatype, " +
                "then the file should have the default extension"
        )
        fun `should handle default extension for response files`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body", contentType = "unknown/no-type")
            setUpInterceptor(mapper, fileType = fileType)

            executeGetRequest("record/request1")

            assertFileExists("$SAVE_FOLDER/record/request1_body_0.txt")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When several matches exist for a request, " +
                "then the body file should have the same index as the request in the scenario"
        )
        fun `should match indexes in descriptor file and actual response file name`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body", contentType = "image/png")
            enqueueServerResponse(200, "body", contentType = "application/json")
            setUpInterceptor(mapper, fileType = fileType)

            executeGetRequest(recordRequestUrl)
            executeGetRequest(recordRequestUrl)

            assertFileExists("$SAVE_FOLDER/record/request_body_0.png")
            assertFileExists("$SAVE_FOLDER/record/request_body_1.json")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a request fails with an exception, " +
                "then the exception should be recorded"
        )
        fun `recording failure should save error in scenario`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            setUpInterceptor(mapper, fileType = fileType)

            val exception = assertThrows<java.net.UnknownHostException> {
                val request = Request.Builder().url("http://falseUrl.wrong/record/error").build()
                client.newCall(request).execute()
            }

            assertFileExists(fileName("record/error", fileType))
            withFile(fileName("record/error", fileType)) {
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

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.interceptor.TestWithServer#mappers")
        @DisplayName(
            "When recording a scenario with request body, " +
                "then the corresponding scenario should be usable as is"
        )
        fun `recorded scenarios should be usable`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            enqueueServerResponse(200, "body")
            setUpInterceptor(mapper, fileType = fileType)
            executeRequest(
                url = recordRequestUrl,
                method = "POST",
                body = """{"some Json content": "some random value"}"""
            )
            interceptor.mode = ENABLED
            val response = executeRequest(
                url = recordRequestUrl,
                method = "POST",
                body = """{"some Json content": "some random value"}"""
            )
            assertResponseCode(response, 200, "OK")
            assertEquals("body", response.body()?.string())
        }
    }

    private val requestUrl = "request"

    private val recordRequestUrl = "record/request"

    private fun setUpInterceptor(
        mapper: Mapper,
        rootFolder: String = SAVE_FOLDER,
        failOnError: Boolean = false,
        fileType: String
    ) {
        val loadingLambda: (String) -> InputStream? = mock {
            on { invoke(any()) } doAnswer { File(SAVE_FOLDER, it.getArgument<String>(0)).inputStream() }
        }

        interceptor = mockInterceptor {
            decodeScenarioPathWith {
                val path = it.url().encodedPath()
                (path + if (path.endsWith("/")) "index.$fileType" else ".$fileType")
                    .drop(1)
            }
            loadFileWith(loadingLambda)
            parseScenariosWith(mapper)
            recordScenariosIn(rootFolder) with MirrorPathPolicy(fileType)
            failOnRecordingError(failOnError)
            setInterceptorStatus(RECORD)
        }

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    private fun fileName(path: String, fileType: String) = "$SAVE_FOLDER/$path.$fileType"

    private fun requestWithNoParams() = Matcher(
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

    private fun requestWithParams() = Matcher(
        RequestDescriptor(
            method = "POST",
            body = "\\QrequestBody\\E",
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

    interface RecorderTestSuite {
        @AfterEach
        fun clearFolder() {
            clearTestFolder()
        }
    }
}

internal const val SAVE_FOLDER = "testFolder"

fun clearTestFolder() {
    val folder = File(SAVE_FOLDER)
    if (folder.exists()) {
        Files.walk(folder.toPath())
            .sorted(Collections.reverseOrder<Any>())
            .map(Path::toFile)
            .forEach { it.delete() }
    }
}
