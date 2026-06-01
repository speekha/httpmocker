/*
 * Copyright 2019-2026 David Blanc
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

import fr.speekha.httpmocker.custom.parser.INCORRECT_FIELD
import fr.speekha.httpmocker.custom.parser.INVALID_BOOLEAN_ERROR
import fr.speekha.httpmocker.custom.parser.INVALID_NUMBER_ERROR
import fr.speekha.httpmocker.custom.parser.JsonParser
import fr.speekha.httpmocker.custom.parser.NO_MORE_TOKEN_ERROR
import fr.speekha.httpmocker.custom.parser.NULL_STRING_VALUE
import fr.speekha.httpmocker.custom.parser.WRONG_END_OF_OBJECT_ERROR
import fr.speekha.httpmocker.custom.parser.WRONG_START_OF_FIELD_NAME_ERROR
import fr.speekha.httpmocker.custom.parser.WRONG_START_OF_LIST_ERROR
import fr.speekha.httpmocker.custom.parser.WRONG_START_OF_OBJECT_ERROR
import fr.speekha.httpmocker.custom.parser.adapters.ObjectAdapter
import fr.speekha.httpmocker.custom.serializer.truncate
import fr.speekha.httpmocker.model.NamedParameter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonParserTest {

    private val mapAdapter = object :
        ObjectAdapter<Map<String, String?>> {
        override fun fromJson(parser: JsonParser): Map<String, String?> {
            val map = mutableMapOf<String, String?>()
            parser.beginObject()
            while (parser.hasNext()) {
                val field = parser.readFieldName()
                val value = parser.readString()
                parser.next()
                map += field to value
            }
            parser.endObject()
            return map
        }
    }

    @Test
    fun `empty input has no next token`() {
        val reader = JsonParser("")
        assertFalse(reader.hasNext())
    }

    @Test
    fun `empty input reading object throws error`() {
        val reader = JsonParser("")
        assertThrowsWithMessage<IllegalStateException>(WRONG_START_OF_OBJECT_ERROR) {
            reader.beginObject()
        }
    }

    @Test
    fun `empty input reading list throws error`() {
        val reader = JsonParser("")
        assertThrowsWithMessage<IllegalStateException>(WRONG_START_OF_LIST_ERROR) {
            reader.beginList()
        }
    }

    @Test
    fun `empty input next throws error`() {
        val reader = JsonParser("")
        assertThrowsWithMessage<IllegalStateException>(NO_MORE_TOKEN_ERROR) {
            reader.next()
        }
    }

    @Test
    fun `empty object is valid`() {
        val reader = JsonParser("{}")
        assertTrue(reader.hasNext())
    }

    @Test
    fun `empty object parsing as list throws error`() {
        val reader = JsonParser("{}")
        val exception: IllegalStateException = assertThrows { reader.beginList() }
        assertEquals("$WRONG_START_OF_LIST_ERROR{}", exception.message)
    }

    @Test
    fun `empty object end detected`() {
        val reader = JsonParser("{}")
        reader.beginObject()
        assertFalse(reader.hasNext())
    }

    @Test
    fun `empty array parsing as object throws error`() {
        val reader = JsonParser("[]")
        val exception: IllegalStateException = assertThrows { reader.beginObject() }
        assertEquals("$WRONG_START_OF_OBJECT_ERROR[]", exception.message)
    }

    @Test
    fun `empty array end detected`() {
        val reader = JsonParser("[]")
        reader.beginList()
        assertFalse(reader.hasNext())
    }

    @Test
    fun `single field name retrieved`() {
        val result = "field"
        val reader = JsonParser("""{"$result" : "value"}""")
        reader.beginObject()
        assertEquals(result, reader.readFieldName())
    }

    @Test
    fun `incorrect field name throws error`() {
        val reader = JsonParser("""{field: "value"}""")
        reader.beginObject()
        assertThrowsWithMessage<IllegalStateException>(WRONG_START_OF_FIELD_NAME_ERROR) {
            reader.readFieldName()
        }
    }

    @Test
    fun `missing colon in field throws error`() {
        val reader = JsonParser("""{"field" "value"}""")
        reader.beginObject()
        assertThrowsWithMessage<IllegalStateException>(INCORRECT_FIELD) {
            reader.readFieldName()
        }
    }

    @Test
    fun `null field name throws error`() {
        val reader = JsonParser("""{null: "value"}""")
        reader.beginObject()
        assertThrowsWithMessage<IllegalStateException>(
            "$WRONG_START_OF_FIELD_NAME_ERROR $NULL_STRING_VALUE"
        ) {
            reader.readFieldName()
        }
    }

    @Test
    fun `integer field value retrieved`() {
        val result = 1152
        val reader = JsonParser("""{"field": 1 152 }""")
        reader.beginObject()
        reader.readFieldName()
        assertEquals(result, reader.readInt())
    }

    @Test
    fun `incorrect integer throws error`() {
        val reader = JsonParser("""{"field": "1 152" }""")
        reader.beginObject()
        reader.readFieldName()
        assertThrowsWithMessage<IllegalStateException>(INVALID_NUMBER_ERROR) {
            reader.readInt()
        }
    }

    @Test
    fun `long field value retrieved`() {
        val result = 1152L
        val reader = JsonParser("""{"field": 1 152 }""")
        reader.beginObject()
        reader.readFieldName()
        assertEquals(result, reader.readLong())
    }

    @Test
    fun `boolean field values retrieved`() {
        val reader = JsonParser("""{"field1": true    , "field2": false }""")
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
    fun `incorrect boolean throws error`() {
        val reader = JsonParser("""{"field": error }""")
        reader.beginObject()
        reader.readFieldName()
        assertThrowsWithMessage<IllegalStateException>(INVALID_BOOLEAN_ERROR) {
            reader.readBoolean()
        }
    }

    @Test
    fun `simple string retrieved`() {
        val reader = JsonParser("""{"field":"a test string"}""")
        reader.beginObject()
        reader.readFieldName()
        assertEquals("a test string", reader.readString())
    }

    @Test
    fun `string with quotes retrieved`() {
        val reader = JsonParser("""{"field":"a test \"string\""}""")
        reader.beginObject()
        reader.readFieldName()
        assertEquals("""a test "string"""", reader.readString())
    }

    @Test
    fun `null string retrieved`() {
        val reader = JsonParser("""{"field": null }""")
        reader.beginObject()
        reader.readFieldName()
        assertNull(reader.readString())
    }

    @Test
    fun `simple object iterable`() {
        val json = """
        {
          "field1": "1",
          "field2": "2"
        }
        """.trimIndent()
        val reader = JsonParser(json)
        with(reader) {
            val list = mutableListOf<NamedParameter>()
            beginObject()
            while (hasNext()) {
                val field = readFieldName()
                val value = readString()
                next()
                list += NamedParameter(field, value)
            }
            assertEquals(
                listOf(NamedParameter("field1", "1"), NamedParameter("field2", "2")),
                list
            )
        }
    }

    @Test
    fun `simple object finishing incorrectly throws error`() {
        val json = """
        {
          "field1": "1",
          "field2": "2"
        }
        """.trimIndent()
        val reader = JsonParser(json)
        with(reader) {
            beginObject()
            readFieldName()
            readString()
            next()
            assertThrowsWithMessage<IllegalStateException>(WRONG_END_OF_OBJECT_ERROR) {
                endObject()
            }
        }
    }

    @Test
    fun `object field retrieved`() {
        val json = """
        {
          "field0" : "0",
          "object" : {
            "field1": "1",
            "field2": "2"
          }
        }
        """.trimIndent()
        val reader = JsonParser(json)
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
    fun `incorrect object field throws error`() {
        val json = """
        {
          "field0" : "0",
          "object" : {
            "field1": "1",
            "field2": "2"
          }
        }
        """.trimIndent()
        val reader = JsonParser(json)
        with(reader) {
            beginObject()
            readFieldName()
            assertThrowsWithMessage<IllegalStateException>(WRONG_START_OF_OBJECT_ERROR) {
                readObject(mapAdapter)
            }
        }
    }

    @Test
    fun `list of integers parsed`() {
        val json = "[1, 2, 3]"
        val list = mutableListOf<Int>()
        with(JsonParser(json)) {
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
    fun `list of strings parsed`() {
        val json = """["1", "2", "3"]"""
        val list = mutableListOf<String?>()
        with(JsonParser(json)) {
            beginList()
            while (hasNext()) {
                list += readString()
                next()
            }
            endList()
        }
        assertEquals(listOf("1", "2", "3"), list as List<String?>)
    }

    @Test
    fun `list of objects parsed`() {
        val json = """
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
        val list = mutableListOf<Map<String, String?>>()
        with(JsonParser(json)) {
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
            ),
            list as List<Map<String, String?>>
        )
    }

    @Test
    fun `list of lists parsed`() {
        val json = "[[1, 2, 3],[1, 2, 3]]"
        val list = mutableListOf<List<Int>>()
        with(JsonParser(json)) {
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
    fun `string truncate empty`() {
        assertEquals("", "".truncate(10))
    }

    @Test
    fun `string truncate no truncation needed`() {
        assertEquals("azertyuiop", "azertyuiop".truncate(10))
    }

    @Test
    fun `string truncate with ellipsis`() {
        assertEquals("azertyu...", "azertyuiopazertyuiol".truncate(10))
    }

    // Helper functions
    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
        try {
            block()
            throw AssertionError("Expected ${T::class.simpleName} but no exception was thrown")
        } catch (e: Throwable) {
            if (e is T) {
                return e
            }
            throw AssertionError("Expected ${T::class.simpleName} but got ${e::class.simpleName}: ${e.message}", e)
        }
    }

    private inline fun <reified T : Throwable> assertThrowsWithMessage(
        expectedMessage: String,
        block: () -> Unit
    ) {
        val exception = assertThrows<T>(block)
        assertTrue(
            exception.message?.startsWith(expectedMessage) == true,
            "Expected message to start with '$expectedMessage' but got '${exception.message}'"
        )
    }
}
