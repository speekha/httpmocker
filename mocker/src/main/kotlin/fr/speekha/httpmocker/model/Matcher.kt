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

package fr.speekha.httpmocker.model

/**
 * Describes a request pattern and the appropriate response for that request
 */
data class Matcher(

    /**
     * The request to match
     */
    val request: RequestDescriptor = RequestDescriptor(),

    /**
     * The mocked response
     */
    val response: ResponseDescriptor,

    /**
     * The mocked error
     */
    val error: NetworkError? = null
) {
    val result: RequestResult
        get() = response ?: error!!
//        ?: NetworkError(
//            "java.io.IOException",
//            "Incorrect response: no response or error associated with request $request"
//        )
}
