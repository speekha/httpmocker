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

package fr.speekha.httpmocker

/**
 * Defines the interceptor's state and how it is supposed to respond to requests (intercept
 * them, let them through or record them)
 */
enum class Mode(private val status: String) {
    /** lets every request through without interception. */
    DISABLED("disabled"),
    /** intercepts all requests and return responses found in a predefined configuration */
    ENABLED("enabled"),
    /** allows to look for responses locally, but execute the request if no response is found */
    MIXED("in mixed mode"),
    /** allows to record actual requests and responses for future use as mock scenarios */
    RECORD("recording");

    override fun toString(): String = status
}
