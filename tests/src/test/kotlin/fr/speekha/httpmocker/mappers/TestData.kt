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

package fr.speekha.httpmocker.mappers

import fr.speekha.httpmocker.io.StreamReader
import fr.speekha.httpmocker.io.asReader
import fr.speekha.httpmocker.io.readAsStringList
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestTemplate
import fr.speekha.httpmocker.model.ResponseDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.InputStream

internal val completeData = listOf(
    Matcher(
        RequestTemplate(
            exactMatch = true,
            protocol = "https",
            method = "post",
            host = "test.com",
            port = 15926,
            path = "/path",
            headers = listOf(
                NamedParameter("reqHeader1", "1"),
                NamedParameter("reqHeader1", "2"),
                NamedParameter("reqHeader2", "3"),
                NamedParameter("reqHeader3", null),
                NamedParameter("Set-Cookie", "\"cookie\"=\"value\"")
            ),
            params = listOf(
                NamedParameter("param1", "1"),
                NamedParameter("param2", "2"),
                NamedParameter("param3", null)
            ),
            body = ".*<1>.*"
        ),
        ResponseDescriptor(
            delay = 50,
            code = 201,
            mediaType = "application/json",
            headers = listOf(
                NamedParameter("resHeader1", "4"),
                NamedParameter("resHeader1", "5"),
                NamedParameter("resHeader2", "6")
            ),
            body = "<simple body />",
            bodyFile = "body_content.txt"
        ),
        NetworkError(
            exceptionType = "java.io.IOException",
            message = "error message"
        )
    )
)

internal val partialData = listOf(
    Matcher(RequestTemplate(), ResponseDescriptor()),
    Matcher(RequestTemplate(), ResponseDescriptor())
)

internal val partialDataError = listOf(
    Matcher(error = NetworkError("SomeExceptionType"))
)

internal fun getCompleteJsonInput(): InputStream = ClassLoader.getSystemClassLoader()
    .getResourceAsStream("complete_input.json") ?: "".byteInputStream()

internal fun getCompleteXmlInput(): InputStream = ClassLoader.getSystemClassLoader()
    .getResourceAsStream("complete_input.xml") ?: "".byteInputStream()

internal fun getPartialJsonInput(): StreamReader = (ClassLoader.getSystemClassLoader()
    .getResourceAsStream("partial_input.json") ?: "".byteInputStream()).asReader()

internal fun getPartialXmlInput(): StreamReader = (ClassLoader.getSystemClassLoader()
    .getResourceAsStream("partial_input.xml") ?: "".byteInputStream()).asReader()

internal fun getPartialJsonInputWithError(): StreamReader = (ClassLoader.getSystemClassLoader()
    .getResourceAsStream("partial_with_error.json") ?: "".byteInputStream()).asReader()

internal fun getPartialXmlInputWithError(): StreamReader = (ClassLoader.getSystemClassLoader()
    .getResourceAsStream("partial_with_error.xml") ?: "".byteInputStream()).asReader()

internal fun getExpectedJsonOutput() = getCompleteJsonInput().readAsStringList()
    .map {
        it.trim()
            .replace(Regex(":[ ]+"), ":")
    }

internal fun getExpectedXmlOutput() = getCompleteXmlInput().readAsStringList()

internal fun getMinimalJsonOutput() =
    """[
        |  {
        |    "request":{
        |      "headers":{},
        |      "params":{}
        |    },
        |    "response":{
        |      "delay":0,
        |      "code":200,
        |      "media-type":"text/plain",
        |      "headers":{},
        |      "body":""
        |    }
        |  },
        |  {
        |    "request":{
        |      "headers":{},"params":{}
        |    },
        |    "error":{
        |      "type":"error"
        |    }
        |  }
        |]"""
        .trimMargin()
        .split("\n")

internal fun getMinimalXmlOutput() =
    """<?xml version="1.0" encoding="UTF-8"?>
        |<scenarios>
        |    <case>
        |        <response delay="0" code="200" media-type="text/plain">
        |            <body></body>
        |        </response>
        |    </case>
        |    <case>
        |        <error type="error" />
        |    </case>
        |</scenarios>"""
        .trimMargin()
        .split("\n")

/**
 * Check equality after removing all the format enhancement added by the different JSON parsers (
 * indentation, line feeds, etc.).
 */
internal fun testJsonStream(expectedResult: List<String>, actual: String) {
    val result = actual.split('\n')
        .joinToString("") {
            it.trim()
                .replace(Regex("\\p{Space}*:\\p{Space}+"), ":")
                .replace(Regex("\\p{Space}*,\\p{Space}+"), ",")
                .replace("[ ", "[")
                .replace(" ]", "]")
                .replace("{ ", "{")
        }
    val expected = expectedResult.joinToString("") { it.trim() }
    assertEquals(expected, result)
}

internal fun testXmlStream(expectedResult: List<String>, actual: String) {
    val expected = expectedResult.joinToString("\n")
    assertEquals(expected, actual)
}
