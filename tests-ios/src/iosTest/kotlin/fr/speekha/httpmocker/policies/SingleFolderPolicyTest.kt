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

import fr.speekha.httpmocker.HTTP_METHOD_POST
import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.serialization.XML_FORMAT
import kotlin.test.Test
import kotlin.test.assertEquals

class SingleFolderPolicyTest {

    @Test
    fun `should store configuration files in a single folder`() {
        val policy: FilingPolicy = SingleFolderPolicy("folder")
        val request = HttpRequest(
            host = "www.somestuff.com",
            path = "/test/with/path",
            method = HTTP_METHOD_POST,
            body = "body",
            headers = listOf(NamedParameter("header", "value")),
        )
        assertEquals("folder/test_with_path.json", policy.getPath(request))
    }

    @Test
    fun `should use the right extension`() {
        val policy: FilingPolicy = SingleFolderPolicy(
            "folder",
            XML_FORMAT
        )
        val request = HttpRequest(
            host = "www.somestuff.com",
            path = "/test/with/path",
            method = HTTP_METHOD_POST,
            body = "body",
            headers = listOf(NamedParameter("header", "value")),
        )
        assertEquals("folder/test_with_path.xml", policy.getPath(request))
    }

    @Test
    fun `should handle empty root folder`() {
        val policy: FilingPolicy = SingleFolderPolicy("")
        val request = HttpRequest(
            host = "www.somestuff.com",
            path = "/test/with/path",
            method = HTTP_METHOD_POST,
            body = "body",
            headers = listOf(NamedParameter("header", "value")),
        )
        assertEquals("test_with_path.json", policy.getPath(request))
    }

    @Test
    fun `should handle empty path segments`() {
        val policy: FilingPolicy = SingleFolderPolicy("folder")
        val request = HttpRequest(
            host = "www.somestuff.com",
            path = "/test/with/path/",
            method = HTTP_METHOD_POST,
            body = "body",
            headers = listOf(NamedParameter("header", "value")),
        )
        assertEquals("folder/test_with_path.json", policy.getPath(request))
    }

    @Test
    fun `should handle empty path URL`() {
        val policy: FilingPolicy = SingleFolderPolicy("folder")
        val request = HttpRequest(
            host = "www.somestuff.com",
            path = "",
            method = HTTP_METHOD_POST,
            body = "body",
            headers = listOf(NamedParameter("header", "value")),
        )
        assertEquals("folder/index.json", policy.getPath(request))
    }
}
