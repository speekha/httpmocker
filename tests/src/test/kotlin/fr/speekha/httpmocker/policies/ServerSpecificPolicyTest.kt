/*
 * Copyright 2019-2020 David Blanc
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
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.serialization.XML_FORMAT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Server Specific Policy")
class ServerSpecificPolicyTest {

    private val policy: FilingPolicy = ServerSpecificPolicy()

    @Nested
    @DisplayName("Given a server specific policy")
    inner class TestPolicy {

        @Test
        @DisplayName("When processing a URL, host should be present in final path")
        fun `should include URL host in path`() {
            val request = HttpRequest(
                host = "www.somestuff.com",
                path = "/test/with/path",
                method = "POST",
                body = "body",
                headers = listOf(Header("header", "value")),
            )
            assertEquals("www.somestuff.com/test/with/path.json", policy.getPath(request))
        }

        @Test
        @DisplayName(
            "When processing a URL ending with a '/', " +
                "then index.json should be added in the last empty segment"
        )
        fun `should handle URL when last segment is empty`() {
            val request = HttpRequest(
                host = "www.somestuff.com",
                path = "/test/with/path/",
                method = "POST",
                body = "body",
                headers = listOf(Header("header", "value")),
            )
            assertEquals("www.somestuff.com/test/with/path/index.json", policy.getPath(request))
        }

        @Test
        @DisplayName(
            "When file format is not JSON, then the proper extension should be used"
        )
        fun `should use proper extension`() {
            val xmlPolicy: FilingPolicy = ServerSpecificPolicy(XML_FORMAT)
            val request = HttpRequest(
                host = "www.somestuff.com",
                path = "/test/with/path/",
                method = "POST",
                body = "body",
                headers = listOf(Header("header", "value")),
            )
            assertEquals("www.somestuff.com/test/with/path/index.xml", xmlPolicy.getPath(request))
        }
    }
}
