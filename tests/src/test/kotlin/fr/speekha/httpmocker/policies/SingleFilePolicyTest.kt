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

import fr.speekha.httpmocker.HTTP_METHOD_POST
import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.model.NamedParameter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Single File Policy")
class SingleFilePolicyTest {

    @Nested
    @DisplayName("Given a path mirroring policy")
    inner class TestPolicy {

        @Test
        @DisplayName("When processing a URL, then file path should always be the same")
        fun `should always return the same file`() {
            val file = "folder/singleFile.json"
            val policy: FilingPolicy = SingleFilePolicy(file)
            val request = HttpRequest(
                host = "www.somestuff.com",
                path = "/test/with/path",
                method = HTTP_METHOD_POST,
                body = "body",
                headers = listOf(NamedParameter("header", "value")),
            )
            Assertions.assertEquals(file, policy.getPath(request))
        }
    }
}
