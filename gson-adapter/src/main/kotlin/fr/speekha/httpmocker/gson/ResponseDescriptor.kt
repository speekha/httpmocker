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

import com.google.gson.annotations.SerializedName
import fr.speekha.httpmocker.BODY
import fr.speekha.httpmocker.BODY_FILE
import fr.speekha.httpmocker.CODE
import fr.speekha.httpmocker.DEFAULT_MEDIA_TYPE
import fr.speekha.httpmocker.DEFAULT_RESPONSE_CODE
import fr.speekha.httpmocker.DELAY
import fr.speekha.httpmocker.HEADERS
import fr.speekha.httpmocker.MEDIA_TYPE

internal data class ResponseDescriptor(

    @SerializedName(DELAY)
    val delay: Long = 0,

    @SerializedName(CODE)
    val code: Int = DEFAULT_RESPONSE_CODE,

    @SerializedName(MEDIA_TYPE)
    val mediaType: String = DEFAULT_MEDIA_TYPE,

    @SerializedName(HEADERS)
    val headers: HeaderAdapter.HeaderList? = HeaderAdapter.HeaderList(),

    @SerializedName(BODY)
    val body: String = "",

    @SerializedName(BODY_FILE)
    val bodyFile: String? = null
)
