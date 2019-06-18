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

package fr.speekha.httpmocker

import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

internal val completeData = listOf(
    Matcher(
        RequestDescriptor(
            method = "post",
            headers = listOf(
                Header("reqHeader1", "1"),
                Header("reqHeader1", "2"),
                Header("reqHeader2", "3")
            ),
            params = mapOf("param" to "1"),
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
        )
    )
)

internal val partialData = listOf(
    Matcher(RequestDescriptor(), ResponseDescriptor()),
    Matcher(RequestDescriptor(), ResponseDescriptor())
)

internal fun getCompleteInput(): InputStream = ClassLoader.getSystemClassLoader()
    .getResourceAsStream("complete_input.json")

internal fun getPartialInput(): InputStream = ClassLoader.getSystemClassLoader()
    .getResourceAsStream("partial_input.json")

internal fun getExpectedOutput() = getCompleteInput().readAsStringList()
    .map {
        it.trim()
            .replace(": ", ":")
            .replace(",", "")
    }

internal fun testStream(expectedResult: List<String>, writeBlock: (OutputStream) -> Unit) {
    val stream = ByteArrayOutputStream()
    stream.use {
        writeBlock(it)
    }
    val result = stream.toByteArray()
        .toString(Charset.forName("UTF-8"))
        .split('\n')
        .joinToString("") {
            it.trim().replace(": ", ":").replace(",", "")
        }
    assertEquals(expectedResult.sumBy { it.length }, result.length)
    assertEquals("", expectedResult.fold(result) { acc, token ->
        acc.replaceFirst(token, "")
    })
}