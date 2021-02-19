/*
 * Copyright 2019-2021 David Blanc
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

package fr.speekha.httpmocker.custom

import fr.speekha.httpmocker.custom.parser.JsonParser
import fr.speekha.httpmocker.custom.parser.adapters.MatcherAdapter
import fr.speekha.httpmocker.custom.serializer.toJson
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.serialization.JSON_FORMAT
import fr.speekha.httpmocker.serialization.Mapper

/**
 * A mapper using custom JSON parsing to serialize/deserialize scenarios.
 */
class CustomMapper : Mapper {

    override val supportedFormat: String = JSON_FORMAT

    private val adapter =
        MatcherAdapter()

    override fun deserialize(payload: String): List<Matcher> = JsonParser(
        payload
    ).parseJson(adapter)

    private fun JsonParser.parseJson(matcherMapper: MatcherAdapter): List<Matcher> {
        beginList()
        val list = populateList(matcherMapper)
        endList()
        return list
    }

    private fun JsonParser.populateList(matcherMapper: MatcherAdapter): List<Matcher> =
        mutableListOf<Matcher>().also { list ->
            while (hasNext()) {
                list += matcherMapper.fromJson(this)
                next()
            }
        }

    override fun serialize(matchers: List<Matcher>): String = matchers.toJson()
}

fun unknownFieldError(field: String): Nothing = error("Unknown field $field")
