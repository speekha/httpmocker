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
import fr.speekha.httpmocker.serialization.XML_FORMAT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Single Folder Policy")
class SingleFolderPolicyTest {

    @Nested
    @DisplayName("Given a path mirroring policy")
    inner class TestPolicy {

        @Test
        @DisplayName("When processing a URL, then resulting file should be " +
                "in the predefined folder and filename should match URL path")
        fun `should store configuration files in a single folder`() {
            val policy: FilingPolicy = SingleFolderPolicy("folder")
            val request = buildRequest(
                "http://www.somestuff.com/test/with/path",
                listOf("header" to "value"),
                "POST",
                "body"
            )
            Assertions.assertEquals("folder/test_with_path.json", policy.getPath(request))
        }

        @Test
        @DisplayName("When scenarios are not in JSON, then resulting file should have" +
                "the proper extension")
        fun `should use the right extension`() {
            val policy: FilingPolicy = SingleFolderPolicy(
                "folder",
                XML_FORMAT
            )
            val request = buildRequest(
                "http://www.somestuff.com/test/with/path",
                listOf("header" to "value"),
                "POST",
                "body"
            )
            Assertions.assertEquals("folder/test_with_path.xml", policy.getPath(request))
        }

        @Test
        @DisplayName("When configured folder is empty, then resulting path should only contain a file name")
        fun `should handle empty root folder`() {
            val policy: FilingPolicy = SingleFolderPolicy("")
            val request = buildRequest(
                "http://www.somestuff.com/test/with/path",
                listOf("header" to "value"),
                "POST",
                "body"
            )
            Assertions.assertEquals("test_with_path.json", policy.getPath(request))
        }

        @Test
        @DisplayName("When processing a URL ending with a '/', then file name should be based on the URL path")
        fun `should handle empty path segments`() {
            val policy: FilingPolicy = SingleFolderPolicy("folder")
            val request = buildRequest(
                "http://www.somestuff.com/test/with/path/",
                listOf("header" to "value"),
                "POST",
                "body"
            )
            Assertions.assertEquals("folder/test_with_path.json", policy.getPath(request))
        }

        @Test
        @DisplayName("When processing a URL with an empty path, then file name should be index.json")
        fun `should handle empty path URL`() {
            val policy: FilingPolicy = SingleFolderPolicy("folder")
            val request = buildRequest(
                "http://www.somestuff.com/",
                listOf("header" to "value"),
                "POST",
                "body"
            )
            Assertions.assertEquals("folder/index.json", policy.getPath(request))
        }
    }
}
