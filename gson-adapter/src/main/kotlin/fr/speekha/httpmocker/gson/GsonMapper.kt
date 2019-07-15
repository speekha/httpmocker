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

package fr.speekha.httpmocker.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.gson.Header as JsonHeader
import fr.speekha.httpmocker.gson.Matcher as JsonMatcher
import fr.speekha.httpmocker.gson.RequestDescriptor as JsonRequestDescriptor
import fr.speekha.httpmocker.gson.ResponseDescriptor as JsonResponseDescriptor


/**
 * A mapper using Gson to serialize/deserialize scenarios.
 */
class GsonMapper : Mapper {

    private val adapter: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(HeaderAdapter.HeaderList::class.java, HeaderAdapter())
        .create()

    private val dataType = MatcherType().type

    override fun deserialize(payload: String): List<Matcher> =
        adapter.parse(payload).map {
            it.toModel()
        }

    private fun Gson.parse(json: String) =
        fromJson<List<JsonMatcher>>(json, dataType) ?: emptyList()

    override fun serialize(matchers: List<Matcher>): String =
        adapter.toJson(matchers.map { it.fromModel() })

    private class MatcherType : TypeToken<List<JsonMatcher>>()

    private fun Matcher.fromModel() = JsonMatcher(request.fromModel(), response.fromModel())

    private fun JsonMatcher.toModel() =
        Matcher(request?.toModel() ?: RequestDescriptor(), response.toModel())

    private fun JsonRequestDescriptor.toModel() =
        RequestDescriptor(protocol, method, host, port, path, headers.toModel(), params, body)

    private fun RequestDescriptor.fromModel() =
        JsonRequestDescriptor(protocol, method, host, port, path, getHeaders(), params, body)

    private fun RequestDescriptor.getHeaders() =
        HeaderAdapter.HeaderList(headers.map { it.fromModel() })

    private fun JsonHeader.toModel() = Header(name, value)

    private fun Header.fromModel() = JsonHeader(name, value)

    private fun JsonResponseDescriptor.toModel() =
        ResponseDescriptor(delay, code, mediaType, headers.toModel(), body, bodyFile)

    private fun HeaderAdapter.HeaderList?.toModel() = this?.map { it.toModel() } ?: emptyList()

    private fun ResponseDescriptor.fromModel() =
        JsonResponseDescriptor(
            delay,
            code,
            mediaType,
            HeaderAdapter.HeaderList().apply {
                addAll(headers.map { it.fromModel() })
            },
            body,
            bodyFile
        )
}

