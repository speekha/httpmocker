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

package fr.speekha.httpmocker.client.okhttp

import fr.speekha.httpmocker.client.TestWithServer
import fr.speekha.httpmocker.okhttp.MockResponseInterceptor
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.jupiter.api.Assertions

open class OkHttpTests : TestWithServer() {

    protected lateinit var interceptor: MockResponseInterceptor

    protected lateinit var client: OkHttpClient

    protected fun assertResponseCode(response: Response, code: Int, message: String) {
        Assertions.assertEquals(code, response.code)
        Assertions.assertEquals(message, response.message)
    }

    fun executeRequest(
        url: String,
        method: String = "GET",
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ): Response {
        val request = buildRequest(completeLocalUrl(url), headers, method, body)
        return client.newCall(request).execute()
    }

    @JvmOverloads
    fun executeRequestSync(
        url: String,
        method: String = "GET",
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ): Response = runBlocking {
        executeRequest(url, method, body, headers)
    }

    private fun buildRequest(
        url: String,
        headers: List<Pair<String, String>> = emptyList(),
        method: String = "GET",
        body: String? = null
    ): Request {
        return Request.Builder()
            .url(url)
            .headers(Headers.headersOf(*headers.flatMap { listOf(it.first, it.second) }.toTypedArray()))
            .method(method, body?.toRequestBody("text/plain".toMediaTypeOrNull()))
            .build()
    }

    fun check404Response(
        url: String,
        method: String = "GET",
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ) {
        assertResponseCode(executeRequest(url, method, body, headers), 404, "Not Found")
    }

    fun checkResponseBody(
        expected: String,
        url: String,
        method: String = "GET",
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ) {
        val response = executeRequest(url, method, body, headers)
        assertResponseCode(response, 200, "OK")
        Assertions.assertEquals(expected, response.body?.string())
    }
}
