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

package fr.speekha.httpmocker.mappers

import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.readAsStringList
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.InputStream

internal val completeData = listOf(
    Matcher(
        RequestDescriptor(
            exactMatch = true,
            protocol = "https",
            method = "post",
            host = "test.com",
            port = 15926,
            path = "/path",
            headers = listOf(
                Header("reqHeader1", "1"),
                Header("reqHeader1", "2"),
                Header("reqHeader2", "3"),
                Header("reqHeader3", null),
                Header("Set-Cookie", "\"cookie\"=\"value\"")
            ),
            params = mapOf("param1" to "1", "param2" to "2", "param3" to null),
            body = ".*1.*"
        ),
        ResponseDescriptor(
            delay = 50,
            code = 201,
            mediaType = "application/json",
            headers = listOf(
                Header("resHeader1", "4"),
                Header("resHeader1", "5"),
                Header("resHeader2", "6")
            ),
            body = "simple body",
            bodyFile = "body_content.txt"
        ),
        NetworkError(
            exceptionType = "java.io.IOException",
            message = "error message"
        )
    )
)

internal val partialData = listOf(
    Matcher(RequestDescriptor(), ResponseDescriptor()),
    Matcher(RequestDescriptor(), ResponseDescriptor())
)

internal val partialDataError = listOf(
    Matcher(error = NetworkError("SomeExceptionType"))
)

internal fun getCompleteInput(): InputStream = ClassLoader.getSystemClassLoader()
    .getResourceAsStream("complete_input.json") ?: "".byteInputStream()

internal fun getPartialInput(): InputStream = ClassLoader.getSystemClassLoader()
    .getResourceAsStream("partial_input.json") ?: "".byteInputStream()

internal fun getPartialInputWithError(): InputStream = ClassLoader.getSystemClassLoader()
    .getResourceAsStream("partial_with_error.json") ?: "".byteInputStream()

internal fun getExpectedOutput() = getCompleteInput().readAsStringList()
    .map {
        it.trim()
            .replace(Regex(":[ ]+"), ":")
    }

internal fun getMinimalOutput() = listOf(
    "[{" +
            "\"request\":{" +
            "\"headers\":{},\"params\":{}" +
            "}," +
            "\"response\":{" +
            "\"delay\":0,\"code\":200,\"media-type\":\"text/plain\",\"headers\":{},\"body\":\"\"" +
            "}}]"
)

/**
 * Check equality after removing all the format enhancement added by the different JSON parsers (
 * indentation, line feeds, etc.).
 */
internal fun testStream(expectedResult: List<String>, actual: String) {
    val result = actual.split('\n')
        .joinToString("") {
            it.trim()
                .replace(Regex("\\p{Space}*:\\p{Space}+"), ":")
                .replace("[ ", "[")
                .replace(" ]", "]")
                .replace("{ ", "{")
        }
    val expected = expectedResult.joinToString("") { it.trim() }
    assertEquals(expected, result)
}
