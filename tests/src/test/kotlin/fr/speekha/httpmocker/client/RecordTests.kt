/*
 * Copyright 2019-2021 David Blanc
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

import fr.speekha.httpmocker.HTTP_METHOD_GET
import fr.speekha.httpmocker.HTTP_METHOD_POST
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.Mode.ENABLED
import fr.speekha.httpmocker.Mode.RECORD
import fr.speekha.httpmocker.NO_RECORDER_ERROR
import fr.speekha.httpmocker.NO_ROOT_FOLDER_ERROR
import fr.speekha.httpmocker.assertThrows
import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.io.StreamReader
import fr.speekha.httpmocker.io.asReader
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestTemplate
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.readAsString
import fr.speekha.httpmocker.serialization.DEFAULT_MEDIA_TYPE
import fr.speekha.httpmocker.serialization.Mapper
import fr.speekha.httpmocker.serialization.readMatches
import fr.speekha.httpmocker.withFile
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Suppress("UNUSED_PARAMETER")
abstract class RecordTests<Response : Any, Client : Any> : HttpClientTester<Response, Client> {

    @Nested
    @DisplayName("Given a mock interceptor with no recorder set")
    inner class NoRecorderSet {
        private lateinit var client: Client

        @Test
        @DisplayName("When building the interceptor in record mode, then an error should occur")
        fun `should not allow to init an interceptor in record mode with no recorder`() = runBlocking {
            val exception = assertThrows<IllegalStateException> { setupProvider(RECORD) }
            assertEquals(NO_ROOT_FOLDER_ERROR, exception.message)
            Assertions.assertFalse(::client.isInitialized)
        }

        @Test
        @DisplayName("When setting the interceptor status to record mode, then an error should occur")
        fun `should not allow to record requests if recorder is not set`() = runBlocking {
            setupProvider()
            val exception = assertThrows<IllegalStateException> {
                changeMockerStatus(RECORD)
            }
            assertEquals(NO_RECORDER_ERROR, exception.message)
        }

        private fun setupProvider(status: Mode = ENABLED) {
            client = setupDynamicConf({ null }, mode = status)
        }
    }

    @Nested
    @DisplayName("Given a mock interceptor")
    inner class PolicyTest : RecorderTestSuite {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a recording policy is set, then it should be used")
        fun `should use recording policy`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            runBlocking {
                val policy: FilingPolicy = mockk {
                    every { getPath(any()) } returns "record_policy.$fileType"
                }

                testInterceptor(mapper, { "wrongPolicy.$fileType" }, policy)
                assertFilesExist("$SAVE_FOLDER/record_policy.$fileType", requestBodyFile)
                val request = slot<HttpRequest>()
                verify {
                    policy.getPath(capture(request))
                }
                confirmVerified(policy)
                assertEquals("/$REQUEST_URL", request.captured.path)
            }
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a recording policy is set as lambda, then it should be used")
        fun `should use recording policy as lambda`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            testInterceptor(mapper, { "wrongPolicy.$fileType" }, { "lambda_policy.$fileType" })
            assertFilesExist("$SAVE_FOLDER/lambda_policy.$fileType", requestBodyFile)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When no recording policy is set, then the read policy should be used")
        fun `should use read policy`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            runBlocking {
                val policy: FilingPolicy = mockk {
                    every { getPath(any()) } returns "read_policy.$fileType"
                }
                testInterceptor(mapper, policy, null)
                assertFilesExist("$SAVE_FOLDER/read_policy.$fileType", requestBodyFile)
                verify { policy.getPath(any()) }
                confirmVerified()
            }
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When no policy is set at all, then a default Mirror path policy should be used")
        fun `should use default policy`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            testInterceptor(mapper, null, null)
            assertFilesExist("$SAVE_FOLDER/request.$fileType", requestBodyFile)
        }

        private suspend fun testInterceptor(mapper: Mapper, readPolicy: FilingPolicy?, writePolicy: FilingPolicy?) {
            enqueueServerResponse(200, BODY)
            setupRecordPolicyConf(mapper, readPolicy, writePolicy)
            executeRequest(REQUEST_URL)
        }

        private val requestBodyFile = "$SAVE_FOLDER/request_body_0.txt"
    }

    @Nested
    @DisplayName("Given a mock interceptor in record mode")
    inner class InterceptionTest : RecorderTestSuite {
        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a request is recorded, then it should not be blocked")
        fun `should let requests through when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY)
            setUpInterceptor(mapper, fileType = fileType)

            checkResponseBody(BODY, RECORD_REQUEST_URL)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When a response with multiple lines is recorded, then all lines should be read")
        fun `should let response with multiple lines through when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            val body = "line 1\nline 2\n".repeat(100)
            enqueueServerResponse(200, body)
            setUpInterceptor(mapper, fileType = fileType)

            checkResponseBody(body, RECORD_REQUEST_URL)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When recording a request fails, then it should not interfere with the request")
        fun `should let requests through when recording even if saving fails`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY)
            blockWritingToOutputFolder()
            setUpInterceptor(mapper, false, fileType)

            checkResponseBody(BODY, RECORD_REQUEST_URL)
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When recording a request fails and errors are expected, " +
                "then the error should be returned"
        )
        fun `recording failure should return an error if desired`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) {
            runBlocking {
                enqueueServerResponse(200, BODY)
                blockWritingToOutputFolder()
                setUpInterceptor(mapper = mapper, failOnError = true, fileType = fileType)

                assertThrows<FileNotFoundException> {
                    executeRequest(RECORD_REQUEST_URL)
                }
            }
        }

        private fun blockWritingToOutputFolder() {
            File(SAVE_FOLDER).mkdir()
            File("$SAVE_FOLDER/record").createNewFile()
        }
    }

    @Nested
    @DisplayName("Given a mock interceptor in record mode with a root folder")
    inner class RecordTest : RecorderTestSuite {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When recording a request, " +
                "then scenario and response body files should be created in that folder"
        )
        fun `should store requests and responses in the proper locations when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY)
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest(RECORD_REQUEST_URL)

            assertFilesExist("$SAVE_FOLDER/record/request.$fileType")
            assertFilesExist("$SAVE_FOLDER/record/request_body_0.txt")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When recording a request for a URL ending with a '/', " +
                "then scenario files should be named with 'index'"
        )
        fun `should name body file correctly when last path segment is empty`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY)
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest("record/")

            assertFilesExist("$SAVE_FOLDER/record/index.$fileType")
            assertFilesExist("$SAVE_FOLDER/record/index_body_0.txt")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName("When recording a request, then content of scenario files should be correct")
        fun `should store requests and responses when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY, listOf(HEADER2))
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest(
                "request?param1=value1",
                HTTP_METHOD_POST,
                "requestBody",
                listOf(HEADER1)
            )

            withFile(fileName(REQUEST_URL, fileType)) {
                val result = mapper.readMatches(it)
                val expectedResult = requestWithParams()
                assertEquals(listOf(expectedResult), result)
            }

            withFile("$SAVE_FOLDER/request_body_0.txt") {
                assertEquals(BODY, it.readAsString())
            }
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When recording a request or response with a null body, " +
                "then body should be empty in scenario files"
        )
        fun `should handle null request and response bodies when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, null)
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest(REQUEST_URL)

            withFile(fileName(REQUEST_URL, fileType)) {
                val result = mapper.readMatches(it)
                val expectedResult = Matcher(
                    RequestTemplate(
                        method = HTTP_METHOD_GET,
                        headers = extraHeaders
                    ),
                    ResponseDescriptor(
                        code = 200,
                        mediaType = DEFAULT_MEDIA_TYPE,
                        headers = listOf(
                            NamedParameter(CONTENT_LENGTH, "0"),
                            NamedParameter(CONTENT_TYPE, DEFAULT_MEDIA_TYPE)
                        )
                    )
                )
                assertEquals(listOf(expectedResult), result)
            }
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When a scenario already exists for a request, " +
                "then the scenario should be completed with the new one"
        )
        fun `should update existing descriptors when recording`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY, listOf(HEADER2))
            enqueueServerResponse(200, "second body")
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest(
                "request?param1=value1",
                HTTP_METHOD_POST,
                "requestBody",
                listOf(HEADER1)
            )
            executeRequest(REQUEST_URL)

            withFile(fileName(REQUEST_URL, fileType)) {
                val result = mapper.readMatches(it)
                val expectedResult = listOf(
                    requestWithParams(),
                    requestWithNoParams()
                )
                assertEquals(expectedResult, result)
            }

            withFile("$SAVE_FOLDER/request_body_0.txt") {
                assertEquals(BODY, it.readAsString())
            }
            withFile("$SAVE_FOLDER/request_body_1.txt") {
                assertEquals("second body", it.readAsString())
            }
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When recording a response body, " +
                "then the file should have the proper extension"
        )
        fun `should add proper extension to response files`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY, contentType = "image/png")
            enqueueServerResponse(200, BODY, contentType = "application/json")
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest("record/request1")
            executeRequest("record/request2")

            assertFilesExist("$SAVE_FOLDER/record/request1_body_0.png")
            assertFilesExist("$SAVE_FOLDER/record/request2_body_0.json")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When recording a response body with a mediatype charset, " +
                "then the file should have the proper extension"
        )
        fun `should handle proper extension for response files`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY, contentType = "application/json; charset=UTF-8")
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest("record/request1")

            assertFilesExist("$SAVE_FOLDER/record/request1_body_0.json")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When recording a response body with unknwown mediatype, " +
                "then the file should have the default extension"
        )
        fun `should handle default extension for response files`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY, contentType = "unknown/no-type")
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest("record/request1")

            assertFilesExist("$SAVE_FOLDER/record/request1_body_0.txt")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When several matches exist for a request, " +
                "then the body file should have the same index as the request in the scenario"
        )
        fun `should match indexes in descriptor file and actual response file name`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY, contentType = "image/png")
            enqueueServerResponse(200, BODY, contentType = "application/json")
            setUpInterceptor(mapper, fileType = fileType)

            executeRequest(RECORD_REQUEST_URL)
            executeRequest(RECORD_REQUEST_URL)

            assertFilesExist("$SAVE_FOLDER/record/request_body_0.png")
            assertFilesExist("$SAVE_FOLDER/record/request_body_1.json")
        }

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When recording a request fails with an exception, " +
                "then the exception should be recorded"
        )
        fun `recording failure should save error in scenario`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            setUpInterceptor(mapper, fileType = fileType)

            val exception = checkIoException {
                executeRequest("http://falseUrl.wrong/record/error")
            }

            assertFilesExist(fileName("record/error", fileType))
            withFile(fileName("record/error", fileType)) {
                val result = mapper.readMatches(it)
                val expectedResult = Matcher(
                    request = RequestTemplate(
                        method = HTTP_METHOD_GET,
                        headers = extraHeaders
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
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When recording a scenario with request body, " +
                "then the corresponding scenario should be usable as is"
        )
        fun `recorded scenarios should be usable`(
            title: String,
            mapper: Mapper,
            fileType: String
        ) = runBlocking {
            enqueueServerResponse(200, BODY)
            setUpInterceptor(mapper, fileType = fileType)
            executeRequest(
                url = RECORD_REQUEST_URL,
                method = HTTP_METHOD_POST,
                body = """{"some Json content": "some random value"}"""
            )
            changeMockerStatus(ENABLED)
            checkResponseBody(
                expected = BODY,
                url = RECORD_REQUEST_URL,
                method = HTTP_METHOD_POST,
                body = """{"some Json content": "some random value"}"""
            )
        }
    }

    abstract suspend fun checkIoException(block: suspend () -> Unit): Throwable

    private fun setUpInterceptor(
        mapper: Mapper,
        failOnError: Boolean = false,
        fileType: String
    ) {
        val loadingLambda: (String) -> StreamReader? = {
            File(SAVE_FOLDER, it).inputStream().asReader()
        }

        setupRecordConf(mapper, loadingLambda, SAVE_FOLDER, failOnError, fileType)
    }

    private fun fileName(path: String, fileType: String) = "$SAVE_FOLDER/$path.$fileType"

    private fun requestWithNoParams() = Matcher(
        RequestTemplate(
            method = HTTP_METHOD_GET,
            headers = extraHeaders
        ),
        ResponseDescriptor(
            code = 200,
            bodyFile = "request_body_1.txt",
            mediaType = DEFAULT_MEDIA_TYPE,
            headers = listOf(
                NamedParameter(CONTENT_LENGTH, "11"),
                NamedParameter(CONTENT_TYPE, DEFAULT_MEDIA_TYPE)
            )
        )
    )

    private fun requestWithParams() = Matcher(
        RequestTemplate(
            method = HTTP_METHOD_POST,
            body = "\\QrequestBody\\E",
            params = listOf(NamedParameter("param1", "value1")),
            headers = listOf(
                NamedParameter(HEADER1_NAME, HEADER_VALUE)
            ) + extraHeaders
        ),
        ResponseDescriptor(
            code = 200,
            bodyFile = "request_body_0.txt",
            mediaType = DEFAULT_MEDIA_TYPE,
            headers = listOf(
                NamedParameter(CONTENT_LENGTH, "4"),
                NamedParameter(CONTENT_TYPE, DEFAULT_MEDIA_TYPE),
                NamedParameter(HEADER2_NAME, HEADER_VALUE)
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

private const val REQUEST_URL = "request"
private const val RECORD_REQUEST_URL = "record/request"
private const val BODY = "body"
private const val HEADER1_NAME = "someHeader"
private const val HEADER2_NAME = "someKey"
private const val HEADER_VALUE = "someValue"
private val HEADER1 = HEADER1_NAME to HEADER_VALUE
private val HEADER2 = HEADER2_NAME to HEADER_VALUE
private const val CONTENT_LENGTH = "Content-Length"
private const val CONTENT_TYPE = "Content-Type"

fun clearTestFolder() {
    val folder = File(SAVE_FOLDER)
    if (folder.exists()) {
        Files.walk(folder.toPath())
            .sorted(Collections.reverseOrder<Any>())
            .map(Path::toFile)
            .forEach { it.delete() }
    }
}
