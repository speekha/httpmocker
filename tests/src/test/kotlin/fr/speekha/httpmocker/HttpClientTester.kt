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

package fr.speekha.httpmocker

import fr.speekha.httpmocker.scenario.RequestCallback
import io.ktor.http.HttpStatusCode

interface HttpClientTester<Response> {

    fun enqueueServerResponseTmp(
        responseCode: Int,
        responseBody: String?,
        headers: List<Pair<String, String>> = listOf(),
        contentType: String? = null
    )

    suspend fun executeRequest(
        url: String,
        method: String = "GET",
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ): Response

    suspend fun check404Response(
        url: String,
        method: String = "GET",
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    )

    suspend fun checkResponseBody(
        expected: String,
        url: String,
        method: String = "GET",
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    )

    fun setupProvider(
        vararg callbacks: RequestCallback,
        status: Mode = Mode.ENABLED
    )

    suspend fun assertResponseBody(expected: String, response: Response)

    fun assertResponseCode(resultCode: HttpStatusCode, response: Response)
}