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

package fr.speekha.httpmocker.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types.newParameterizedType
import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.model.Matcher


/**
 * A mapper using Moshi to serialize/deserialize scenarios.
 */
class MoshiMapper : Mapper {
    private val adapter: JsonAdapter<List<Matcher>>

    init {
        val moshi = Moshi.Builder()
            .add(HeaderAdapter())
            .add(MatcherAdapter())
            .build()
        adapter = moshi.adapter(
            newParameterizedType(List::class.java, Matcher::class.java)
        )
    }

    override fun fromJson(json: String): List<Matcher> = adapter.fromJson(json) ?: emptyList()

    override fun toJson(matchers: List<Matcher>): String = adapter.toJson(matchers)
}