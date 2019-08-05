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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fr.speekha.httpmocker.model.RequestDescriptor as Model

@Serializable
internal data class RequestDescriptor(
    @SerialName("exact-match")
    val exactMatch: Boolean? = null,
    val protocol: String? = null,
    val method: String? = null,
    val host: String? = null,
    val port: Int? = null,
    val path: String? = null,
    val headers: List<Header>? = null,
    val params: Map<String, String?>? = null,
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