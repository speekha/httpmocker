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

import fr.speekha.httpmocker.EXCEPTION_MESSAGE
import fr.speekha.httpmocker.EXCEPTION_TYPE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fr.speekha.httpmocker.model.NetworkError as Model

@Serializable
internal data class NetworkError(

    @SerialName(EXCEPTION_TYPE)
    val mediaType: String = "",

    @SerialName(EXCEPTION_MESSAGE)
    val body: String? = null
) {
    constructor(model: Model) : this(
        model.exceptionType,
        model.message
    )
}
