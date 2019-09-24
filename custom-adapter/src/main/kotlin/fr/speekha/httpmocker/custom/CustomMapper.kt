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

package fr.speekha.httpmocker.custom

import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.model.Matcher

/**
 * A mapper using custom JSON parsing to serialize/deserialize scenarios.
 */
class CustomMapper : Mapper {

    private val adapter = MatcherAdapter()

    override fun deserialize(payload: String): List<Matcher> = JsonStringReader(payload).parseJson(adapter)

    private fun JsonStringReader.parseJson(matcherMapper: MatcherAdapter): List<Matcher> {
        beginList()
        val list = populateList(matcherMapper)
        endList()
        return list
    }

    private fun JsonStringReader.populateList(matcherMapper: MatcherAdapter): List<Matcher> =
        mutableListOf<Matcher>().also { list ->
            while (hasNext()) {
                list += matcherMapper.fromJson(this)
                next()
            }
        }

    override fun serialize(matchers: List<Matcher>): String = matchers.toJson()
}

fun unknownFieldError(field: String): Nothing = error("Unknown field $field")
