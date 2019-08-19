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
import com.squareup.moshi.JsonClass
import fr.speekha.httpmocker.NAME
import fr.speekha.httpmocker.VALUE

@JsonClass(generateAdapter = true)
internal data class Header(
    @field:Json(name = NAME)
    val name: String = "",

    @field:Json(name = VALUE)
    var value: String? = null
)
