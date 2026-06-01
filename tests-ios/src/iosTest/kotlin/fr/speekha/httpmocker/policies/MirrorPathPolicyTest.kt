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

package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.serialization.JSON_FORMAT
import fr.speekha.httpmocker.serialization.XML_FORMAT
import kotlin.test.Test
import kotlin.test.assertEquals

class MirrorPathPolicyTest {

    private val policy: FilingPolicy = MirrorPathPolicy(JSON_FORMAT)

    @Test
    fun `should mirror URL`() {
        val request = HttpRequest(
            host = "www.somestuff.com",
            path = "/test/with/path",
            method = "POST",
            body = "body",
            headers = listOf(NamedParameter("header", "value")),
        )
        assertEquals("test/with/path.json", policy.getPath(request))
    }

    @Test
    fun `should add index to path`() {
        val request = HttpRequest(
            host = "www.somestuff.com",
            path = "/test/with/path/",
            method = "POST",
            body = "body",
            headers = listOf(NamedParameter("header", "value")),
        )
        assertEquals("test/with/path/index.json", policy.getPath(request))
    }

    @Test
    fun `should use proper extensions`() {
        val xmlPolicy: FilingPolicy = MirrorPathPolicy(XML_FORMAT)

        val request = HttpRequest(
            host = "www.somestuff.com",
            path = "/test/with/path/",
            method = "POST",
            body = "body",
            headers = listOf(NamedParameter("header", "value")),
        )
        assertEquals("test/with/path/index.xml", xmlPolicy.getPath(request))
    }
}
