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

package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.buildRequest
import fr.speekha.httpmocker.serialization.JSON_FORMAT
import fr.speekha.httpmocker.serialization.XML_FORMAT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Mirror Path Policy")
class MirrorPathPolicyTest {

    val policy: FilingPolicy = MirrorPathPolicy(JSON_FORMAT)

    @Nested
    @DisplayName("Given a path mirroring policy")
    inner class TestPolicy {
        @Test
        @DisplayName("When processing a URL, then file path should be kept from the URL")
        fun `should mirror URL`() {
            val request = buildRequest(
                "http://www.somestuff.com/test/with/path",
                listOf("header" to "value"),
                "POST",
                "body"
            )
            assertEquals("test/with/path.json", policy.getPath(request))
        }

        @Test
        @DisplayName("When processing a URL ending with a '/', " +
                "then index.json should be added in the last empty segment")
        fun `should add index to path`() {
            val request = buildRequest(
                "http://www.somestuff.com/test/with/path/",
                listOf("header" to "value"),
                "POST",
                "body"
            )
            assertEquals("test/with/path/index.json", policy.getPath(request))
        }

        @Test
        @DisplayName("When file format is not JSON, " +
                "then the proper extension should be used")
        fun `should use proper extensions`() {
            val xmlPolicy: FilingPolicy = MirrorPathPolicy(XML_FORMAT)

            val request = buildRequest(
                "http://www.somestuff.com/test/with/path/",
                listOf("header" to "value"),
                "POST",
                "body"
            )
            assertEquals("test/with/path/index.xml", xmlPolicy.getPath(request))
        }
    }
}
