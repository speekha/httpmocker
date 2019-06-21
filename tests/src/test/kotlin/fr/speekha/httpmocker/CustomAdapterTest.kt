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

package fr.speekha.httpmocker

import fr.speekha.httpmocker.custom.CustomAdapter
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.charset.Charset

class CustomAdapterTest : AbstractJsonMapperTest(CustomAdapter()) {
    @Test
    fun `step by step`() {
        val json = """[
  {
    "request": {},
    "response": {}
  },
  {
    "response": {}
  }
]"""
        val mapper = CustomAdapter()
        assertEquals(
            listOf(
                Matcher(response = ResponseDescriptor()),
                Matcher(RequestDescriptor(), ResponseDescriptor())
            ), mapper.readMatches(json.byteInputStream(Charset.forName("UTF-8")))
        )
    }
}