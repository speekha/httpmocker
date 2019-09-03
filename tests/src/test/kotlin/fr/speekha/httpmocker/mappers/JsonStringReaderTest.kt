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

import fr.speekha.httpmocker.custom.INVALID_BOOLEAN_ERROR
import fr.speekha.httpmocker.custom.INVALID_NUMBER_ERROR
import fr.speekha.httpmocker.custom.JsonStringReader
import fr.speekha.httpmocker.custom.NO_FIELD_ID_ERROR
import fr.speekha.httpmocker.custom.NO_MORE_TOKEN_ERROR
import fr.speekha.httpmocker.custom.ObjectAdapter
import fr.speekha.httpmocker.custom.WRONG_END_OF_LIST_ERROR
import fr.speekha.httpmocker.custom.WRONG_END_OF_OBJECT_ERROR
import fr.speekha.httpmocker.custom.WRONG_START_OF_LIST_ERROR
import fr.speekha.httpmocker.custom.WRONG_START_OF_OBJECT_ERROR
import fr.speekha.httpmocker.custom.WRONG_START_OF_STRING_FIELD_ERROR
import fr.speekha.httpmocker.custom.truncate
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.StringStartsWith
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@DisplayName("Custom JSON parser")
class JsonStringReaderTest {

    @Nested
    @DisplayName("Given an empty input")
    inner class EmptyInput {

        val reader = JsonStringReader("")
        @Test
        fun `When parsing input, then no new token should be found`() {
            assertFalse(reader.hasNext())
        }

        @Test
        fun `When reading an object, then an error should occur`() {
            assertError<IllegalStateException>(WRONG_START_OF_OBJECT_ERROR) { reader.beginObject() }
        }

        @Test
        fun `When reading a list, then an error should occur`() {
            assertError<IllegalStateException>(WRONG_START_OF_LIST_ERROR) { reader.beginList() }
        }

        @Test
        fun `When trying to iterate, then an error should occur`() {
            assertError<IllegalStateException>(NO_MORE_TOKEN_ERROR) { reader.next() }
        }
    }

    @Nested
    @DisplayName("Given an empty object as input")
    inner class EmptyObject {

        val reader = JsonStringReader("{}")

        @Test
        fun `When parsing the object, then input should be valid`() {
            assertTrue(reader.hasNext())
        }

        @Test
        fun `When parsing as a list, then an error should occur`() {
            val exception = assertThrows<IllegalStateException> { reader.beginList() }
            assertEquals("$WRONG_START_OF_LIST_ERROR{}", exception.message)
        }

        @Test
        fun `When parsing the object, then the end of the object should be detected`() {
            reader.beginObject()
            assertFalse(reader.hasNext())
        }
    }

    @Nested
    @DisplayName("Given an empty array as input")
    inner class EmptyArray {

        val reader = JsonStringReader("[]")

        @Test
        fun `When parsing as an object, then an error should occur`() {
            val exception = assertThrows<IllegalStateException> { reader.beginObject() }
            assertEquals("$WRONG_START_OF_OBJECT_ERROR[]", exception.message)
        }

        @Test
        fun `When parsing the array, then the end of the list should be detected`() {
            reader.beginList()
            assertFalse(reader.hasNext())
        }
    }

    @Nested
    @DisplayName("Given an object with a single field as input")
    inner class Field {

        @Test
        fun `When reading the field name, then the proper string should be returned`() {
            val result = "field"
            val reader = JsonStringReader("{\"$result\" : \"value\"}")
            reader.beginObject()
            assertEquals(result, reader.readFieldName())
        }

        @Test
        fun `When reading an incorrect field name, then an error should occur`() {
            val reader = JsonStringReader("{\"field\" \"value\"}")
            reader.beginObject()
            assertError<IllegalStateException>(NO_FIELD_ID_ERROR) { reader.readFieldName() }
        }

        @Test
        fun `When reading a null field name, then an error should occur`() {
            val reader = JsonStringReader("{null: \"value\"}")
            reader.beginObject()
            assertError<IllegalStateException>(NO_FIELD_ID_ERROR) { reader.readFieldName() }
        }
    }

    @Nested
    @DisplayName("Given an object with a numeric field as input")
    inner class NumericField {

        @Test
        fun `When field is an integer, then its value should be retrieved`() {
            val result = 1152
            val reader = JsonStringReader("{\"field\": 1 152 }")
            reader.beginObject()
            reader.readFieldName()
            assertEquals(result, reader.readInt())
        }

        @Test
        fun `When reading an incorrect integer, then an error should occur`() {
            val reader = JsonStringReader("{\"field\": \"1 152\" }")
            reader.beginObject()
            reader.readFieldName()
            assertError<IllegalStateException>(INVALID_NUMBER_ERROR) { reader.readInt() }
        }

        @Test
        fun `When field is a long, then its value should be retrieved`() {
            val result = 1152L
            val reader = JsonStringReader("{\"field\": 1 152 }")
            reader.beginObject()
            reader.readFieldName()
            assertEquals(result, reader.readLong())
        }
    }

    @Nested
    @DisplayName("Given an object with a Boolean field as input")
    inner class BooleanField {

        @Test
        fun `When field is a boolean, then its value should be retrieved`() {
            val reader = JsonStringReader("{\"field1\": true    , \"field2\": false }")
            reader.beginObject()
            reader.readFieldName()
            val field1 = reader.readBoolean()
            reader.next()
            reader.readFieldName()
            val field2 = reader.readBoolean()
            assertEquals(true, field1)
            assertEquals(false, field2)
        }

        @Test
        fun `When reading an incorrect boolean, then an error should occur`() {
            val reader = JsonStringReader("{\"field\": error }")
            reader.beginObject()
            reader.readFieldName()
            assertError<IllegalStateException>(INVALID_BOOLEAN_ERROR) { reader.readBoolean() }
        }
    }

    @Nested
    @DisplayName("Given an object with a String field as input")
    inner class StringField {

        @Test
        fun `When String is simple, its value should be retrieved`() {
            val result = "a test string"
            val reader = JsonStringReader("{\"field\":\"$result\"}")
            reader.beginObject()
            reader.readFieldName()
            assertEquals(result, reader.readString())
        }

        @Test
        fun `When String contains quotes, its value should be retrieved`() {
            val result = """a test \"string\""""
            val reader = JsonStringReader("{\"field\":\"$result\"}")
            reader.beginObject()
            reader.readFieldName()
            assertEquals("""a test "string"""", reader.readString())
        }

        @Test
        fun `When String is null, null should be retrieved`() {
            val reader = JsonStringReader("{\"field\": null }")
            reader.beginObject()
            reader.readFieldName()
            assertNull(reader.readString())
        }

        @Test
        fun `When String contains JSON special characters, its value should be retrieved`() {
            val result = """ { [ \" , } ] """
            val reader = JsonStringReader("{\"field\":\"$result\"}")
            reader.beginObject()
            reader.readFieldName()
            assertEquals(""" { [ " , } ] """, reader.readString())
        }

        @ParameterizedTest(name = "Incorrect value: {0}")
        @MethodSource("fr.speekha.httpmocker.mappers.JsonStringReaderTest#stringErrors")
        fun `When String is incorrect, an error should occur`(input: String, output: String) {
            val reader = JsonStringReader(input)
            assertError<IllegalStateException>(output) { reader.readString() }
        }
    }

    @Nested
    @DisplayName("Given a simple object as input")
    inner class SimpleObject {

        private val reader = JsonStringReader(simpleObject)

        @Test
        fun `When parsing the object, then fields should be iterable`() {
            with(reader) {
                val list = mutableListOf<Pair<String, String?>>()
                beginObject()
                while (hasNext()) {
                    val field = readFieldName()
                    val value = readString()
                    next()
                    list += field to value
                }
                assertEquals(listOf("field1" to "1", "field2" to "2"), list)
            }
        }

        @Test
        fun `When finishing the object incorrectly, then an error should occur`() {
            with(reader) {
                beginObject()
                readFieldName()
                readString()
                next()
                assertError<IllegalStateException>(WRONG_END_OF_OBJECT_ERROR) { endObject() }
            }
        }
    }

    @Nested
    @DisplayName("Given an object with an Object field as input and a corresponding adapter")
    inner class ObjectField {

        private val reader = JsonStringReader(complexObject)

        @Test
        fun `When reading the field, then the object should be retrieved`() {
            with(reader) {
                val obj = mutableMapOf<String, Any>()
                beginObject()
                obj[readFieldName()] = readString() ?: error("Incorrect object name")
                next()
                obj[readFieldName()] = readObject(mapAdapter)
                next()
                assertEquals(
                    mapOf("field0" to "0", "object" to mapOf("field1" to "1", "field2" to "2")),
                    obj
                )
            }
        }

        @Test
        fun `When object is incorrect, an error should occur`() {
            with(JsonStringReader(complexObject)) {
                beginObject()
                readFieldName()
                assertError<IllegalStateException>(WRONG_START_OF_OBJECT_ERROR) {
                    readObject(mapAdapter)
                }
            }
        }
    }

    @Nested
    @DisplayName("Given an object with an array field as input")
    inner class ArrayField {

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
            val json = """["1", "2", "3"]"""
            val list = mutableListOf<String?>()
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
            val list = mutableListOf<Map<String, String?>>()
            with(JsonStringReader(simpleList)) {
                beginList()
                while (hasNext()) {
                    val map = mutableMapOf<String, String?>()
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
            assertEquals(
                listOf(
                    mapOf("field1" to "1", "field2" to "2"),
                    mapOf("field1" to "1", "field2" to "2")
                ), list
            )
        }

        @Test
        fun `should detect when list is not entirely processed`() {
            with(JsonStringReader(simpleList)) {
                beginList()
                val map = mutableMapOf<String, String?>()
                beginObject()
                while (hasNext()) {
                    val field = readFieldName()
                    val value = readString()
                    next()
                    map[field] = value
                }
                endObject()
                assertError<IllegalStateException>(WRONG_END_OF_LIST_ERROR) { endList() }
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
    }

    @ParameterizedTest(name = "When input is \"{0}\", then result should be \"{1}\"")
    @MethodSource("truncateData")
    @DisplayName("Given a String to truncate")
    fun truncateTests(input: String, output: String) {
        assertEquals(output, input.truncate(10))
    }

    private val mapAdapter = object : ObjectAdapter<Map<String, String?>> {
        override fun fromJson(reader: JsonStringReader): Map<String, String?> {
            val map = mutableMapOf<String, String?>()
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
              "field0" : "0",
              "object" : {
                "field1": "1",
                "field2": "2"
              }
            }
            """.trimIndent()

        @JvmStatic
        fun truncateData(): Stream<Arguments> = listOf(
            arrayOf("", ""),
            arrayOf("azertyuiop", "azertyuiop"),
            arrayOf("azertyuiopazertyuiol", "azertyu...")
        ).map { Arguments.of(*it) }.stream()

        @JvmStatic
        fun stringErrors(): Stream<Arguments> = listOf(
            arrayOf("{a test string}", "$WRONG_START_OF_STRING_FIELD_ERROR{a test..."),
            arrayOf("{\"a test string\"}", "$WRONG_START_OF_STRING_FIELD_ERROR{\"a tes...")
        ).map { Arguments.of(*it) }.stream()

        inline fun <reified E : Throwable> assertError(
            message: String,
            noinline block: () -> Unit
        ) {
            val exception = assertThrows<E>(block)
            assertThat(exception.message, StringStartsWith(message))
        }
    }
}
