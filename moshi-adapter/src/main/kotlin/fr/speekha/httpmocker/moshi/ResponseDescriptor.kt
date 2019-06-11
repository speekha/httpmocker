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

import com.squareup.moshi.Json

data class ResponseDescriptor(

    val delay: Long = 0,

    val code: Int = 200,

    @field:Json(name = "media-type")
    val mediaType: String = "text/plain",

    val headers: List<Header> = emptyList(),

    val body: String = "",

    @field:Json(name = "body-file")
    val bodyFile: String? = null
)