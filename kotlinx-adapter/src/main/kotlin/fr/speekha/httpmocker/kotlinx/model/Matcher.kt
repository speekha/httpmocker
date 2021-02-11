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

import fr.speekha.httpmocker.serialization.ERROR
import fr.speekha.httpmocker.serialization.REQUEST
import fr.speekha.httpmocker.serialization.RESPONSE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fr.speekha.httpmocker.model.Matcher as Model

@Serializable
internal data class Matcher(

    @SerialName(REQUEST)
    val request: RequestDescriptor? = null,

    @SerialName(RESPONSE)
    val response: ResponseDescriptor? = null,

    @SerialName(ERROR)
    val error: NetworkError? = null
) {
    constructor(model: Model) : this(
        RequestDescriptor(model.request),
        model.response?.let {
            ResponseDescriptor(
                it
            )
        },
        model.error?.let { NetworkError(it) }
    )
}
