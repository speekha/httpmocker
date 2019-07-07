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

import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.ResponseDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

abstract class AbstractJsonMapperTest(val mapper: Mapper) {

    @Test
    fun `should parse a JSON file`() {
        val result = mapper.readMatches(getCompleteInput())
        assertEquals(completeData, result)
    }

    @Test
    fun `should populate default values properly`() {
        val result = mapper.readMatches(getPartialInput())
        assertEquals(partialData, result)
    }


    @Test
    fun `should handle headers with colons`() {
        val json = """[
  {
    "response": {
      "headers": {
        "Location": "http://www.google.com"
      }
    }
  }
]"""

        assertEquals(
            listOf(
                Matcher(
                    response = ResponseDescriptor(
                        headers = listOf(
                            Header("Location", "http://www.google.com")
                        )
                    )
                )
            ), mapper.readMatches(json.byteInputStream())
        )
    }

    @Test
    fun `should write a proper JSON file`() {
        val expected = getExpectedOutput()
        testStream(expected, mapper.toJson(listOf(completeData[0])))
    }

    @Test
    fun `should write a proper minimum JSON file`() {
        val expected = getMinimalOutput()
        testStream(expected, mapper.toJson(listOf(Matcher(response = ResponseDescriptor()))))
    }
}