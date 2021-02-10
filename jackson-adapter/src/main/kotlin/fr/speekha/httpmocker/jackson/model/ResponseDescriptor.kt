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
import fr.speekha.httpmocker.jackson.serialization.KeyValueDeserializer
import fr.speekha.httpmocker.jackson.serialization.KeyValueSerializer
import fr.speekha.httpmocker.serialization.*

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class ResponseDescriptor
@JsonCreator constructor(

    @JsonProperty(DELAY)
    val delay: Long = 0,

    @JsonProperty(CODE)
    val code: Int = DEFAULT_RESPONSE_CODE,

    @JsonProperty(MEDIA_TYPE)
    val mediaType: String = DEFAULT_MEDIA_TYPE,

    @JsonProperty(HEADERS)
    @JsonDeserialize(using = KeyValueDeserializer::class)
    @JsonSerialize(using = KeyValueSerializer::class)
    val headers: List<KeyValue> = emptyList(),

    @JsonProperty(BODY)
    val body: String = "",

    @JsonProperty(BODY_FILE)
    val bodyFile: String? = null
)
