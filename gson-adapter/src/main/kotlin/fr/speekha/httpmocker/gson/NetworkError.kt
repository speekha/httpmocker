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
import fr.speekha.httpmocker.DEFAULT_EXCEPTION
import fr.speekha.httpmocker.EXCEPTION_MESSAGE
import fr.speekha.httpmocker.EXCEPTION_TYPE

internal data class NetworkError(

    @SerializedName(EXCEPTION_TYPE)
    val exceptionType: String = DEFAULT_EXCEPTION,

    @SerializedName(EXCEPTION_MESSAGE)
    val message: String = ""
)
