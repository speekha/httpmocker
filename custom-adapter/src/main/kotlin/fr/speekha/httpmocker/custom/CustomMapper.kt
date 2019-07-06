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

    override fun fromJson(json: String): List<Matcher> = JsonStringReader(json).parseJson(adapter)

    private fun JsonStringReader.parseJson(matcherMapper: MatcherAdapter): MutableList<Matcher> {
        val list = mutableListOf<Matcher>()
        beginList()
        while (hasNext()) {
            list += matcherMapper.fromJson(this)
            next()
        }
        endList()
        return list
    }

    override fun toJson(matchers: List<Matcher>): String = compactJson(matchers.toJson())

}