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

import fr.speekha.httpmocker.serialization.JsonFormatConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("UNUSED_PARAMETER")
@DisplayName("JSON format conversion")
class JsonFormatConverterTest {

    @ParameterizedTest(name = "When input {0}, then output should be properly formatted")
    @MethodSource("dataImport")
    @DisplayName("Given a proper JSON stream to read")
    fun readConversionTest(title: String, input: String, output: String) {
        assertEquals(output, JsonFormatConverter().expand(input))
    }

    @ParameterizedTest(name = "When input {0}, then output should be properly formatted")
    @MethodSource("dataExport")
    @DisplayName("Given a proper JSON stream to write")
    fun writeConversionTest(title: String, input: String, output: String) {
        assertEquals(output, JsonFormatConverter().compact(input))
    }

    companion object {

        private val testTitles =
            listOf(
                "is a minimal JSON",
                "contains an empty header list",
                "contains one header",
                "contains duplicate headers"
            )

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
                  },
                  "params": {
                    "param1": "value1"
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
                  },
                  "params": {
                    "param1": "value1",
                    "param1": "value2"
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
                  "params": [
                  ]
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
                  ],
                  "params": [
                    {
                      "name": "param1",
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
                  ],
                  "params": [
                    {
                      "name": "param1",
                      "value": "value1"
                    },
                    {
                      "name": "param1",
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

        @JvmStatic
        fun dataImport(): Stream<Arguments> = testTitles.zip(
            commonFormat zip kotlinxFormat
        ) { a, (b, c) -> Arguments.of(a, b, c) }
            .stream()

        @JvmStatic
        fun dataExport(): Stream<Arguments> = testTitles.zip(
            kotlinxFormat zip commonFormat
        ) { a, (b, c) -> Arguments.of(a, b, c) }
            .stream()
    }
}
