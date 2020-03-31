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

package fr.speekha.httpmocker.moshi.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.speekha.httpmocker.serialization.BODY
import fr.speekha.httpmocker.serialization.BODY_FILE
import fr.speekha.httpmocker.serialization.CODE
import fr.speekha.httpmocker.serialization.DEFAULT_MEDIA_TYPE
import fr.speekha.httpmocker.serialization.DEFAULT_RESPONSE_CODE
import fr.speekha.httpmocker.serialization.DELAY
import fr.speekha.httpmocker.serialization.HEADERS
import fr.speekha.httpmocker.serialization.MEDIA_TYPE

@JsonClass(generateAdapter = true)
internal data class ResponseDescriptor(

    @field:Json(name = DELAY)
    val delay: Long = 0,

    @field:Json(name = CODE)
    val code: Int = DEFAULT_RESPONSE_CODE,

    @field:Json(name = MEDIA_TYPE)
    val mediaType: String = DEFAULT_MEDIA_TYPE,

    @field:Json(name = HEADERS)
    val headers: List<Header> = emptyList(),

    @field:Json(name = BODY)
    val body: String = "",

    @field:Json(name = BODY_FILE)
    val bodyFile: String? = null
)
