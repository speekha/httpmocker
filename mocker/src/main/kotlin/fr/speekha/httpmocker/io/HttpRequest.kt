/*
 * Copyright 2019-2020 David Blanc
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

package fr.speekha.httpmocker.io

import fr.speekha.httpmocker.model.Header

data class HttpRequest @JvmOverloads constructor(
    val method: String = "GET",
    val scheme: String = "http",
    val host: String = "",
    val port: Int = 80,
    val path: String = "",
    val params: Map<String, String> = emptyMap(),
    val headers: List<Header> = emptyList(),
    val body: String? = null,
) {
    val pathSegments: List<String>
        get() {
            val segments = if (path.startsWith("/")) path.drop(1) else path
            return segments.split("/")
        }
}
