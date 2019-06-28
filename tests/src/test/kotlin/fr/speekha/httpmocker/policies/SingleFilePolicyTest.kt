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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SingleFilePolicyTest {

    @Test
    fun `should always return the same file`() {
        val file = "folder/singleFile.json"
        val policy: FilingPolicy = SingleFilePolicy(file)
        val request = buildRequest(
            "http://www.somestuff.com/test/with/path", listOf("header" to "value"), "POST", "body"
        )
        Assertions.assertEquals(file, policy.getPath(request))
    }
}