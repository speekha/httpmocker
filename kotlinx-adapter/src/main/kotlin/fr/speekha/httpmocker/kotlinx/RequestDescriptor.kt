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

package fr.speekha.httpmocker.kotlinx

import fr.speekha.httpmocker.serialization.BODY
import fr.speekha.httpmocker.serialization.EXACT_MATCH
import fr.speekha.httpmocker.serialization.HEADERS
import fr.speekha.httpmocker.serialization.HOST
import fr.speekha.httpmocker.serialization.METHOD
import fr.speekha.httpmocker.serialization.PARAMS
import fr.speekha.httpmocker.serialization.PATH
import fr.speekha.httpmocker.serialization.PORT
import fr.speekha.httpmocker.serialization.PROTOCOL
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fr.speekha.httpmocker.model.RequestDescriptor as Model

@Serializable
internal data class RequestDescriptor(
    @SerialName(EXACT_MATCH)
    val exactMatch: Boolean? = null,
    @SerialName(PROTOCOL)
    val protocol: String? = null,
    @SerialName(METHOD)
    val method: String? = null,
    @SerialName(HOST)
    val host: String? = null,
    @SerialName(PORT)
    val port: Int? = null,
    @SerialName(PATH)
    val path: String? = null,
    @SerialName(HEADERS)
    val headers: List<Header>? = null,
    @SerialName(PARAMS)
    val params: Map<String, String?>? = null,
    @SerialName(BODY)
    val body: String? = null
) {
    constructor(model: Model) : this(
        model.exactMatch.takeIf { it },
        model.protocol,
        model.method,
        model.host,
        model.port,
        model.path,
        model.headers.map { Header(it) },
        model.params,
        model.body
    )
}
