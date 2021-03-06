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

package fr.speekha.httpmocker.moshi.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.speekha.httpmocker.serialization.BODY
import fr.speekha.httpmocker.serialization.EXACT_MATCH
import fr.speekha.httpmocker.serialization.HEADERS
import fr.speekha.httpmocker.serialization.HOST
import fr.speekha.httpmocker.serialization.METHOD
import fr.speekha.httpmocker.serialization.PARAMS
import fr.speekha.httpmocker.serialization.PATH
import fr.speekha.httpmocker.serialization.PORT
import fr.speekha.httpmocker.serialization.PROTOCOL

@JsonClass(generateAdapter = true)
internal data class RequestDescriptor(

    @field:Json(name = EXACT_MATCH)
    val exactMatch: Boolean? = null,

    @field:Json(name = PROTOCOL)
    val protocol: String? = null,

    @field:Json(name = METHOD)
    val method: String? = null,

    @field:Json(name = HOST)
    val host: String? = null,

    @field:Json(name = PORT)
    val port: Int? = null,

    @field:Json(name = PATH)
    val path: String? = null,

    @field:Json(name = HEADERS)
    val headers: List<KeyValue> = emptyList(),

    @field:Json(name = PARAMS)
    val params: List<KeyValue> = emptyList(),

    @field:Json(name = BODY)
    val body: String? = null

)
