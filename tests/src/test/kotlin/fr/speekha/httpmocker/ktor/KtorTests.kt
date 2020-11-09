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

package fr.speekha.httpmocker.ktor

import fr.speekha.httpmocker.TestWithServer
import fr.speekha.httpmocker.ktor.io.readBytes
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions

open class KtorTests : TestWithServer() {

    protected lateinit var client: HttpClient

    protected suspend inline fun <reified T> executeRequest(
        url: String,
        method: HttpMethod = HttpMethod.Get,
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ): T = client.request(completeLocalUrl(url)) {
        this.method = method
        body?.let {
            this.body = it
        }
        headers {
            headers.forEach { (key, value) ->
                append(key, value)
            }
        }
    }

    protected suspend fun check404Response(
        url: String,
        method: HttpMethod = HttpMethod.Get,
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ) {
        Assertions.assertEquals(
            HttpStatusCode.NotFound,
            executeRequest<HttpResponse>(url, method, body, headers).status
        )
    }

    protected suspend fun checkResponseBody(
        expected: String,
        url: String,
        method: HttpMethod = HttpMethod.Get,
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ) {
        val response = executeRequest<HttpResponse>(url, method, body, headers)
        Assertions.assertEquals(HttpStatusCode.OK, response.status)
        assertResponseBody(expected, response)
    }

    suspend fun assertResponseBody(expected: String, response: HttpResponse) {
        Assertions.assertEquals(expected, String(response.content.readBytes()))
    }
}
