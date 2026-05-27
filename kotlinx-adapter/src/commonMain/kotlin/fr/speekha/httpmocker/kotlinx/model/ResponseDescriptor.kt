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

package fr.speekha.httpmocker.kotlinx.model

import fr.speekha.httpmocker.serialization.BODY
import fr.speekha.httpmocker.serialization.BODY_FILE
import fr.speekha.httpmocker.serialization.CODE
import fr.speekha.httpmocker.serialization.DELAY
import fr.speekha.httpmocker.serialization.HEADERS
import fr.speekha.httpmocker.serialization.MEDIA_TYPE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fr.speekha.httpmocker.model.ResponseDescriptor as Model

@Serializable
internal data class ResponseDescriptor(
    @SerialName(DELAY)
    val delay: Long? = null,
    @SerialName(CODE)
    val code: Int? = null,
    @SerialName(MEDIA_TYPE)
    val mediaType: String? = null,
    @SerialName(HEADERS)
    val headers: List<KeyValue>? = null,
    @SerialName(BODY)
    val body: String? = null,
    @SerialName(BODY_FILE)
    val bodyFile: String? = null
) {
    constructor(model: Model) : this(
        model.delay,
        model.code,
        model.mediaType,
        model.headers.map { KeyValue(it) },
        model.body,
        model.bodyFile
    )
}
