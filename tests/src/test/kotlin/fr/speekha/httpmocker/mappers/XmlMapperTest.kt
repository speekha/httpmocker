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

import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.readMatches
import fr.speekha.httpmocker.sax.SaxParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("UNUSED_PARAMETER")
class XmlMapperTest {

    private val mapper = SaxParser()

    @Nested
    @DisplayName("Given a XML stream to parse")
    inner class ParseXml {

        @Test
        fun `When input is a comprehensive file, then a fully populated object should be returned`() {
            val result = mapper.readMatches(getCompleteXmlInput())
            assertEquals(completeData, result)
        }

        @Test
        fun `When input is a partial scenario, then default values should be used`() {
            val result = mapper.readMatches(getPartialXmlInput())
            assertEquals(partialData, result)
        }

        @Test
        fun `When input is a partial scenario with error, then default values should be used`() {
            val result = mapper.readMatches(getPartialXmlInputWithError())
            assertEquals(partialDataError, result)
        }
    }

    @Nested
    @DisplayName("Given a scenario to write")
    inner class WriteXml {

        @Test
        fun `When input is minimal, then null fields should be omitted`() {
            val expected = getMinimalXmlOutput()
            testXmlStream(
                expected,
                mapper.serialize(
                    listOf(
                        Matcher(response = ResponseDescriptor()),
                        Matcher(error = NetworkError("error"))
                    )
                )
            )
        }

        @Test
        fun `When input is a complete object, the all fields should be properly written`() {
            val expected = getExpectedXmlOutput()
            testXmlStream(expected, mapper.serialize(listOf(completeData[0])))
        }
    }
}
