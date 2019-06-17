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

package fr.speekha.httpmocker.mappers

import fr.speekha.httpmocker.custom.compactJson
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JacksonMapperTest : AbstractJsonMapperTest(JacksonMapper()) {
    @Test
    fun `should handle json`() {
        val matcher = Matcher(
            RequestDescriptor(method = "GET"),
            ResponseDescriptor(
                code = 200,
                body = "in memory response",
                mediaType = "text/plain"
            )
        )

        val str = """[
  {
    "request":  {
      "method":  "GET",
      "headers":  {
      },
      "params":  {
      }
    },
    "response":  {
      "delay":  0,
      "code":  200,
      "media-type":  "text/plain",
      "headers":  {
      },
      "body":  "in memory response"
    }
  }
]"""
        val json = mapper.toJson(listOf(matcher)).also { println("Json: $it") }
        mapper.fromJson(str)
        mapper.fromJson(json)
        Assertions.assertEquals(compactJson(str).replace (":  ", ":"), compactJson(json))
    }
}