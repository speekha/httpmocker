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

package fr.speekha.httpmocker.jackson.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import fr.speekha.httpmocker.jackson.serialization.HeadersDeserializer
import fr.speekha.httpmocker.jackson.serialization.HeadersSerializer
import fr.speekha.httpmocker.serialization.BODY
import fr.speekha.httpmocker.serialization.EXACT_MATCH
import fr.speekha.httpmocker.serialization.HEADERS
import fr.speekha.httpmocker.serialization.HOST
import fr.speekha.httpmocker.serialization.METHOD
import fr.speekha.httpmocker.serialization.PARAMS
import fr.speekha.httpmocker.serialization.PATH
import fr.speekha.httpmocker.serialization.PORT
import fr.speekha.httpmocker.serialization.PROTOCOL

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class RequestDescriptor
@JsonCreator
constructor(

    @JsonProperty(EXACT_MATCH)
    val exactMatch: Boolean? = null,

    @JsonProperty(PROTOCOL)
    val protocol: String? = null,

    @JsonProperty(METHOD)
    val method: String? = null,

    @JsonProperty(HOST)
    val host: String? = null,

    @JsonProperty(PORT)
    val port: Int? = null,

    @JsonProperty(PATH)
    val path: String? = null,

    @JsonProperty(HEADERS)
    @JsonDeserialize(using = HeadersDeserializer::class)
    @JsonSerialize(using = HeadersSerializer::class)
    val headers: List<Header> = emptyList(),

    @JsonProperty(PARAMS)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val params: Map<String, String?> = emptyMap(),

    @JsonProperty(BODY)
    val body: String? = null

)
