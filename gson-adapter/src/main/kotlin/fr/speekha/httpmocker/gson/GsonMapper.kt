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

package fr.speekha.httpmocker.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.serialization.JSON_FORMAT
import fr.speekha.httpmocker.serialization.Mapper
import fr.speekha.httpmocker.gson.model.Matcher as JsonMatcher

/**
 * A mapper using Gson to serialize/deserialize scenarios.
 */
class GsonMapper : Mapper {

    override val supportedFormat: String = JSON_FORMAT

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .registerTypeAdapter(HeaderAdapter.HeaderList::class.java, HeaderAdapter())
        .registerTypeAdapter(ParamsAdapter.ParamList::class.java, ParamsAdapter())
        .create()

    private val dataType = MatcherType().type

    override fun deserialize(payload: String): List<Matcher> = gson.parse(payload).map {
        it.toModel()
    }

    private fun Gson.parse(json: String) =
        fromJson<List<JsonMatcher>>(json, dataType) ?: emptyList()

    override fun serialize(matchers: List<Matcher>): String = gson.toJson(
        matchers.map {
            it.fromModel()
        }
    )

    private class MatcherType : TypeToken<List<JsonMatcher>>()
}
