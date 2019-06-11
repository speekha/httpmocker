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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

abstract class AbstractJsonMapperTest {

    abstract val mapper: Mapper

    @Test
    fun `should parse a JSON file`() {
        val result = mapper.readMatches(getInput())
        assertEquals(data, result)
    }

    @Test
    fun `should write a proper JSON file`() {
        val expected = getExpectedOutput()
        testStream(expected) {
            mapper.writeValue(it, listOf(data[0]))
        }
    }
}