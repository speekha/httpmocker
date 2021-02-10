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

sealed class RequestResult

/**
 * Describes all the elements of a mocked response.
 */
data class ResponseDescriptor(

    /**
     * Simulated delay for this response
     */
    val delay: Long = 0,

    /**
     * HTTP response code
     */
    val code: Int = 200,

    /**
     * Content type
     */
    val mediaType: String = "text/plain",

    /**
     * List of headers
     */
    val headers: List<NamedParameter> = emptyList(),

    /**
     * Response body, if bodyFile is null
     */
    val body: String = "",

    /**
     * File to use to load the response body (only used for static scenarios)
     */
    val bodyFile: String? = null
) : RequestResult()

/**
 * Describes a mocked error happening while answering a request.
 */
data class NetworkError(

    /**
     * The type of the exception to throw
     */
    val exceptionType: String = "java.lang.IllegalStateException",

    /**
     * The exception message
     */
    val message: String? = null
) : RequestResult()
