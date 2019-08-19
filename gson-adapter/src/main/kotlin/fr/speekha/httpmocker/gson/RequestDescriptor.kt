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
import fr.speekha.httpmocker.EXACT_MATCH
import fr.speekha.httpmocker.HEADERS
import fr.speekha.httpmocker.HOST
import fr.speekha.httpmocker.METHOD
import fr.speekha.httpmocker.PARAMS
import fr.speekha.httpmocker.PATH
import fr.speekha.httpmocker.PORT
import fr.speekha.httpmocker.PROTOCOL

internal data class RequestDescriptor(

    @SerializedName(EXACT_MATCH)
    val exactMatch: Boolean? = null,

    @SerializedName(PROTOCOL)
    val protocol: String? = null,

    @SerializedName(METHOD)
    val method: String? = null,

    @SerializedName(HOST)
    val host: String? = null,

    @SerializedName(PORT)
    val port: Int? = null,

    @SerializedName(PATH)
    val path: String? = null,

    @SerializedName(HEADERS)
    val headers: HeaderAdapter.HeaderList? = HeaderAdapter.HeaderList(),

    @SerializedName(PARAMS)
    val params: ParamsAdapter.ParamList = ParamsAdapter.ParamList(),

    @SerializedName(BODY)
    val body: String? = null

)
