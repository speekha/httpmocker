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

import fr.speekha.httpmocker.custom.INVALID_NUMBER_ERROR
import fr.speekha.httpmocker.custom.JsonStringReader
import fr.speekha.httpmocker.custom.ObjectAdapter
import fr.speekha.httpmocker.custom.WRONG_END_OF_LIST_ERROR
import fr.speekha.httpmocker.custom.WRONG_END_OF_OBJECT_ERROR
import fr.speekha.httpmocker.custom.WRONG_START_OF_LIST_ERROR
import fr.speekha.httpmocker.custom.WRONG_START_OF_OBJECT_ERROR
import fr.speekha.httpmocker.custom.WRONG_START_OF_STRING_FIELD_ERROR
import fr.speekha.httpmocker.custom.truncate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class JsonStringReaderTest {


    @Test
    fun `should parse empty string`() {
        val reader = JsonStringReader("")
        assertFalse(reader.hasNext())
    }

    @Test
    fun `should handle empty object`() {
        val reader = JsonStringReader("{}")
        assertTrue(reader.hasNext())
    }

    @Test
    fun `should handle end of string instead of object`() {
        val reader = JsonStringReader("")
        val exception = assertThrows<IllegalStateException> { reader.beginObject() }
        assertEquals(WRONG_START_OF_OBJECT_ERROR, exception.message)
    }

    @Test
    fun `should handle end of string instead of list`() {
        val reader = JsonStringReader("")
        val exception = assertThrows<IllegalStateException> { reader.beginList() }
        assertEquals(WRONG_START_OF_LIST_ERROR, exception.message)
    }

    @Test
    fun `should handle erroneous start of object`() {
        val reader = JsonStringReader("[]")
        val exception = assertThrows<IllegalStateException> { reader.beginObject() }
        assertEquals("$WRONG_START_OF_OBJECT_ERROR[]", exception.message)
    }

    @Test
    fun `should handle erroneous start of list`() {
        val reader = JsonStringReader("{}")
        val exception = assertThrows<IllegalStateException> { reader.beginList() }
        assertEquals("$WRONG_START_OF_LIST_ERROR{}", exception.message)
    }

    @Test
    fun `should detect end of object`() {
        val reader = JsonStringReader("{}")
        reader.beginObject()
        assertFalse(reader.hasNext())
    }

    @Test
    fun `should detect end of list`() {
        val reader = JsonStringReader("[]")
        reader.beginList()
        assertFalse(reader.hasNext())
    }

    @Test
    fun `should read field name`() {
        val result = "field"
        val reader = JsonStringReader("{\"$result\" : \"value\"}")
        reader.beginObject()
        assertEquals(result, reader.readFieldName())
    }

    @Test
    fun `should read string`() {
        val result = "a test string"
        val reader = JsonStringReader("{\"field\":\"$result\"}")
        reader.beginObject()
        reader.readFieldName()
        assertEquals(result, reader.readString())
    }

    @ParameterizedTest
    @MethodSource("stringErrors")
    fun `should detect error on read string`(input: String, output: String) {
        val reader = JsonStringReader(input)
        val exception = assertThrows<IllegalStateException> { reader.readString() }
        assertEquals(output, exception.message)
    }

    @Test
    fun `should read only int field in object`() {
        val result = 1152
        val reader = JsonStringReader("{\"field\": 1 152 }")
        reader.beginObject()
        reader.readFieldName()
        assertEquals(result, reader.readInt())
    }

    @Test
    fun `should detect error on read int`() {
        val reader = JsonStringReader("{\"field\": \"1 152\" }")
        reader.beginObject()
        reader.readFieldName()
        val exception = assertThrows<IllegalStateException> { reader.readInt() }
        assertEquals("$INVALID_NUMBER_ERROR \"1 152\" }", exception.message)
    }

    @Test
    fun `should read only long field in object`() {
        val result = 1152L
        val reader = JsonStringReader("{\"field\": 1 152 }")
        reader.beginObject()
        reader.readFieldName()
        assertEquals(result, reader.readLong())
    }

    @Test
    fun `should iterate through object`() {
        val reader = JsonStringReader(simpleObject)
        val list = mutableListOf<Pair<String, String>>()
        reader.beginObject()
        while (reader.hasNext()) {
            val field = reader.readFieldName()
            val value = reader.readString()
            reader.next()
            list += field to value
        }
        assertEquals(listOf("field1" to "1", "field2" to "2"), list)
    }

    @Test
    fun `should detect when object is not entirely processed`() {
        with(JsonStringReader(simpleObject)) {
            beginObject()
            readFieldName()
            readString()
            next()
            val exception = assertThrows<IllegalStateException> { endObject() }
            assertEquals("$WRONG_END_OF_OBJECT_ERROR\n" +
                    "  \"fie...", exception.message)
        }
    }

    @Test
    fun `should iterate through list of integers`() {
        val json = "[1, 2, 3]"
        val list = mutableListOf<Int>()
        with(JsonStringReader(json)) {
            beginList()
            while (hasNext()) {
                list += readInt()
                next()
            }
            endList()
        }
        assertEquals(listOf(1, 2, 3), list)
    }

    @Test
    fun `should iterate through list of strings`() {
        val json = "[\"1\", \"2\", \"3\"]"
        val list = mutableListOf<String>()
        with(JsonStringReader(json)) {
            beginList()
            while (hasNext()) {
                list += readString()
                next()
            }
            endList()
        }
        assertEquals(listOf("1", "2", "3"), list)
    }

    @Test
    fun `should iterate through list`() {
        val list = mutableListOf<Map<String, String>>()
        with(JsonStringReader(simpleList)) {
            beginList()
            while (hasNext()) {
                val map = mutableMapOf<String, String>()
                beginObject()
                while (hasNext()) {
                    val field = readFieldName()
                    val value = readString()
                    next()
                    map[field] = value
                }
                endObject()
                list += map
                next()
            }
            endList()
        }
        assertEquals(listOf(mapOf("field1" to "1", "field2" to "2"), mapOf("field1" to "1", "field2" to "2")), list)
    }

    @Test
    fun `should detect when list is not entirely processed`() {
        with(JsonStringReader(simpleList)) {
            beginList()
            val map = mutableMapOf<String, String>()
            beginObject()
            while (hasNext()) {
                val field = readFieldName()
                val value = readString()
                next()
                map[field] = value
            }
            endObject()
            val exception = assertThrows<IllegalStateException> { endList() }
            assertEquals("$WRONG_END_OF_LIST_ERROR,\n" +
                    "  {\n" +
                    " ...", exception.message)
        }
    }

    @Test
    fun `should iterate through list of lists`() {
        val json = "[[1, 2, 3],[1, 2, 3]]"
        val list = mutableListOf<List<Int>>()
        with(JsonStringReader(json)) {
            beginList()
            while (hasNext()) {
                val sublist = mutableListOf<Int>()
                beginList()
                while (hasNext()) {
                    sublist += readInt()
                    next()
                }
                list += sublist
                endList()
                next()
            }
            endList()
        }
        assertEquals(listOf(listOf(1, 2, 3), listOf(1, 2, 3)), list)
    }


    @Test
    fun `should read object field`() {
        val reader = JsonStringReader(complexObject)
        val obj = mutableMapOf<String, Any>()
        reader.beginObject()
        obj[reader.readFieldName()] = reader.readString()
        obj[reader.readFieldName()] = reader.readObject(mapAdapter)
        reader.next()
        assertEquals(mapOf("field0" to "0", "object" to mapOf("field1" to "1", "field2" to "2")), obj)
    }

    @Test
    fun `should handle error when read object field`() {
        with(JsonStringReader(complexObject)) {
            beginObject()
            readFieldName()
            val exception = assertThrows<IllegalStateException> {
                readObject(mapAdapter)
            }
            assertEquals("$WRONG_START_OF_OBJECT_ERROR \"0\"\n" +
                    "  ...", exception.message)
        }
    }

    @ParameterizedTest
    @MethodSource("truncateData")
    fun `should truncate strings properly`(input: String, output: String) {
        assertEquals(output, input.truncate(10))
    }

    private val mapAdapter = object : ObjectAdapter<Map<String, String>> {
        override fun fromJson(reader: JsonStringReader): Map<String, String> {
            val map = mutableMapOf<String, String>()
            reader.beginObject()
            while (reader.hasNext()) {
                val field = reader.readFieldName()
                val value = reader.readString()
                reader.next()
                map += field to value
            }
            reader.endObject()
            return map
        }
    }

    companion object {
        private const val SAVE_FOLDER = "testFolder"

        val simpleObject = """
            {
              "field1": "1",
              "field2": "2"
            }
            """.trimIndent()

        val simpleList = """
          [
            {
              "field1": "1",
              "field2": "2"
            },
            {
              "field1": "1",
              "field2": "2"
            }
          ]
            """.trimIndent()

        val complexObject = """
            {
              "field0" : "0"
              "object" : {
                "field1": "1",
                "field2": "2"
              }
            }
            """.trimIndent()

        @JvmStatic
        fun stringErrors(): Stream<Arguments> = listOf(
            arrayOf("{a test string}", "$WRONG_START_OF_STRING_FIELD_ERROR{a test..."),
            arrayOf("{\"a test string\"}", "$WRONG_START_OF_STRING_FIELD_ERROR{\"a tes...")
        ).map { Arguments.of(*it) }.stream()

        @JvmStatic
        fun truncateData(): Stream<Arguments> = listOf(
            arrayOf("", ""),
            arrayOf("azertyuiop", "azertyuiop"),
            arrayOf("azertyuiopazertyuiol", "azertyu...")
        ).map { Arguments.of(*it) }.stream()

    }
}