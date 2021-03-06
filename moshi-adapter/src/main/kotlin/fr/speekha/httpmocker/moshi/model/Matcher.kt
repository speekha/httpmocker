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
import fr.speekha.httpmocker.serialization.ERROR
import fr.speekha.httpmocker.serialization.REQUEST
import fr.speekha.httpmocker.serialization.RESPONSE

@JsonClass(generateAdapter = true)
internal data class Matcher(

    @field:Json(name = REQUEST)
    val request: RequestDescriptor = RequestDescriptor(),

    @field:Json(name = RESPONSE)
    val response: ResponseDescriptor? = null,

    @field:Json(name = ERROR)
    val error: NetworkError? = null
)
