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

import fr.speekha.httpmocker.kotlinx.JsonFormatConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class JsonFormatConverterTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("dataImport")
    fun `should format JSON properly for reading`(title: String, input: String, output: String) {
        assertEquals(output, JsonFormatConverter().import(input))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dataExport")
    fun `should format JSON properly for writing`(title: String, input: String, output: String) {
        assertEquals(output, JsonFormatConverter().export(input))
    }

    companion object {

        @JvmStatic
        fun dataImport(): Stream<Arguments> = testTitles.zip(
            commonFormat zip kotlinxFormat
        ) { a, (b, c) -> Arguments.of(a, b, c) }
            .stream()

        @JvmStatic
        fun dataExport(): Stream<Arguments> = testTitles.zip(
            kotlinxFormat  zip commonFormat) { a, (b, c) -> Arguments.of(a, b, c) }
            .stream()

        private val testTitles = listOf("Minimal JSON", "Empty header list", "One header", "Duplicate headers")

        private val commonFormat = listOf(
            """
            [
              {
                "request": {},
                "response": {}
              },
              {
                "response": {}
              }
            ]
            """.trimIndent(),
            """
            [
              {
                "request": {
                  "method": "GET",
                  "headers": {
                  },
                  "params": {
                  }
                },
                "response": {
                  "delay": 0,
                  "code": 200,
                  "media-type": "text/plain",
                  "headers": {
                  },
                  "body": "a body"
                }
              }
            ]
            """.trimIndent(),
            """
            [
              {
                "request": {
                  "headers": {
                    "header1": "value1"
                  }
                },
                "response": {}
              },
              {
                "response": {}
              }
            ]
            """.trimIndent(),
            """
            [
              {
                "request": {
                  "headers": {
                    "header1": "value1",
                    "header1": "value2"
                  }
                },
                "response": {
                  "headers": {
                    "header1": "value1",
                    "header1": "value2"
                  }
                }
              }
            ]
            """.trimIndent()
        )

        private val kotlinxFormat = listOf(
            """
            [
              {
                "request": {},
                "response": {}
              },
              {
                "response": {}
              }
            ]
            """.trimIndent(),
            """
            [
              {
                "request": {
                  "method": "GET",
                  "headers": [
                  ],
                  "params": {
                  }
                },
                "response": {
                  "delay": 0,
                  "code": 200,
                  "media-type": "text/plain",
                  "headers": [
                  ],
                  "body": "a body"
                }
              }
            ]
            """.trimIndent(),
            """
            [
              {
                "request": {
                  "headers": [
                    {
                      "name": "header1",
                      "value": "value1"
                    }
                  ]
                },
                "response": {}
              },
              {
                "response": {}
              }
            ]
            """.trimIndent(),
            """
            [
              {
                "request": {
                  "headers": [
                    {
                      "name": "header1",
                      "value": "value1"
                    },
                    {
                      "name": "header1",
                      "value": "value2"
                    }
                  ]
                },
                "response": {
                  "headers": [
                    {
                      "name": "header1",
                      "value": "value1"
                    },
                    {
                      "name": "header1",
                      "value": "value2"
                    }
                  ]
                }
              }
            ]
            """.trimIndent()
        )
    }
}