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
import fr.speekha.httpmocker.model.ResponseDescriptor as Model

@Serializable
internal data class ResponseDescriptor(
    val delay: Long? = null,
    val code: Int? = null,
    @SerialName("media-type")
    val mediaType: String? = null,
    val headers: List<Header>? = null,
    val body: String? = null,
    @SerialName("body-file")
    val bodyFile: String? = null
) {
    constructor(model: Model) : this(
        model.delay,
        model.code,
        model.mediaType,
        model.headers.map { Header(it) },
        model.body,
        model.bodyFile
    )
}