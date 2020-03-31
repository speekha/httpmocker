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

package fr.speekha.httpmocker.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.serialization.JSON_FORMAT
import fr.speekha.httpmocker.serialization.Mapper
import fr.speekha.httpmocker.jackson.model.Matcher as JsonMatcher

/**
 * A mapper using Jackson to serialize/deserialize scenarios.
 */
class JacksonMapper : Mapper {

    override val supportedFormat: String = JSON_FORMAT

    private val mapper: ObjectMapper = jacksonObjectMapper()
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT)

    private val matcherTypeRef = jacksonTypeRef<List<JsonMatcher>>()

    override fun deserialize(payload: String): List<Matcher> =
        mapper.readValue<List<JsonMatcher>>(payload, matcherTypeRef).toModel()

    override fun serialize(matchers: List<Matcher>): String =
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(matchers.map { it.fromModel() })

    private fun List<JsonMatcher>.toModel() = map { it.toModel() }
}
