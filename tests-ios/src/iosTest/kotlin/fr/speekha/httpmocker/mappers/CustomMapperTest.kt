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

import fr.speekha.httpmocker.custom.parser.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomMapperTest {

    @Test
    fun `simple JSON object parsed`() {
        val json = """{"protocol":"https","code":201}"""
        val parser = JsonParser(json)

        parser.beginObject()
        assertEquals("protocol", parser.readFieldName())
        assertEquals("https", parser.readString())

        parser.next()
        assertEquals("code", parser.readFieldName())
        assertEquals(201, parser.readInt())
        parser.endObject()
    }

    @Test
    fun `JSON array of objects parsed`() {
        val json = """[{"id":1,"name":"test"},{"id":2,"name":"test2"}]"""
        val parser = JsonParser(json)

        parser.beginList()

        // First object
        assertTrue(parser.hasNext())
        parser.beginObject()
        assertEquals("id", parser.readFieldName())
        assertEquals(1, parser.readInt())
        parser.next()
        assertEquals("name", parser.readFieldName())
        assertEquals("test", parser.readString())
        parser.endObject()

        parser.next()

        // Second object
        assertTrue(parser.hasNext())
        parser.beginObject()
        assertEquals("id", parser.readFieldName())
        assertEquals(2, parser.readInt())
        parser.next()
        assertEquals("name", parser.readFieldName())
        assertEquals("test2", parser.readString())
        parser.endObject()

        parser.endList()
    }

    @Test
    fun `JSON with nested objects parsed`() {
        val json = """{"request":{"method":"post","path":"/api"},"response":{"code":200}}"""
        val parser = JsonParser(json)

        parser.beginObject()

        assertEquals("request", parser.readFieldName())
        parser.beginObject()
        assertEquals("method", parser.readFieldName())
        assertEquals("post", parser.readString())
        parser.next()
        assertEquals("path", parser.readFieldName())
        assertEquals("/api", parser.readString())
        parser.endObject()

        parser.next()
        assertEquals("response", parser.readFieldName())
        parser.beginObject()
        assertEquals("code", parser.readFieldName())
        assertEquals(200, parser.readInt())
        parser.endObject()

        parser.endObject()
    }

    @Test
    fun `JSON with headers object parsed`() {
        val json = """{"response":{"headers":{"Content-Type":"application/json","Authorization":null}}}"""
        val parser = JsonParser(json)

        parser.beginObject()
        assertEquals("response", parser.readFieldName())
        parser.beginObject()
        assertEquals("headers", parser.readFieldName())
        parser.beginObject()

        assertEquals("Content-Type", parser.readFieldName())
        assertEquals("application/json", parser.readString())

        parser.next()
        assertEquals("Authorization", parser.readFieldName())
        // null value
        val nullValue = parser.readString()
        assertEquals(null, nullValue)

        parser.endObject()
        parser.endObject()
        parser.endObject()
    }

    @Test
    fun `JSON with URL in header value parsed`() {
        val json = """{"headers":{"Location":"http://www.google.com"}}"""
        val parser = JsonParser(json)

        parser.beginObject()
        assertEquals("headers", parser.readFieldName())
        parser.beginObject()
        assertEquals("Location", parser.readFieldName())
        assertEquals("http://www.google.com", parser.readString())
        parser.endObject()
        parser.endObject()
    }

    @Test
    fun `JSON with quoted string values parsed`() {
        val json = """{"cookie":"\"name\"=\"value\""}"""
        val parser = JsonParser(json)

        parser.beginObject()
        assertEquals("cookie", parser.readFieldName())
        assertEquals(""""name"="value"""", parser.readString())
        parser.endObject()
    }

    @Test
    fun `JSON with boolean values parsed`() {
        val json = """{"exact-match":true,"active":false}"""
        val parser = JsonParser(json)

        parser.beginObject()
        assertEquals("exact-match", parser.readFieldName())
        assertTrue(parser.readBoolean())

        parser.next()
        assertEquals("active", parser.readFieldName())
        assertEquals(false, parser.readBoolean())
        parser.endObject()
    }

    @Test
    fun `JSON with integer values parsed`() {
        val json = """{"port":8080,"delay":100,"retries":3}"""
        val parser = JsonParser(json)

        parser.beginObject()
        assertEquals("port", parser.readFieldName())
        assertEquals(8080, parser.readInt())

        parser.next()
        assertEquals("delay", parser.readFieldName())
        assertEquals(100, parser.readInt())

        parser.next()
        assertEquals("retries", parser.readFieldName())
        assertEquals(3, parser.readInt())

        parser.endObject()
    }
}
