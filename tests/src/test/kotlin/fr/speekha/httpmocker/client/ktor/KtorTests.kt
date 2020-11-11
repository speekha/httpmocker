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

package fr.speekha.httpmocker.client.ktor

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.builder.FileLoader
import fr.speekha.httpmocker.client.HttpClientTester
import fr.speekha.httpmocker.client.TestWithServer
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.scenario.RequestCallback
import fr.speekha.httpmocker.serialization.Mapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.hamcrest.MatcherAssert
import org.hamcrest.core.StringStartsWith
import org.junit.jupiter.api.Assertions.assertEquals

open class KtorTests : TestWithServer(), HttpClientTester<HttpResponse> {

    protected lateinit var client: HttpClient

    override suspend fun executeRequest(
        url: String,
        method: String,
        body: String?,
        headers: List<Pair<String, String>>
    ): HttpResponse = client.request(completeLocalUrl(url)) {
        this.method = HttpMethod.parse(method)
        body?.let {
            this.body = it
        }
        headers {
            headers.forEach { (key, value) ->
                append(key, value)
            }
        }
    }

    override suspend fun check404Response(
        url: String,
        method: String,
        body: String?,
        headers: List<Pair<String, String>>
    ) {
        assertEquals(
            HttpStatusCode.NotFound,
            executeRequest(url, method, body, headers).status
        )
    }

    override suspend fun checkResponseBody(
        expected: String,
        url: String,
        method: String,
        body: String?,
        headers: List<Pair<String, String>>
    ) {
        val response = executeRequest(url, method, body, headers)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(expected, response.readText())
    }

    override fun setupProviders(
        vararg callbacks: RequestCallback,
        status: Mode
    ) {
        client = mockableHttpClient(CIO) {
            mock {
                setInterceptorStatus(status)
                callbacks.forEach {
                    useDynamicMocks(it)
                }
            }
        }
    }

    override fun setupInterceptor(
        mode: Mode,
        loadingLambda: FileLoader,
        mapper: Mapper,
        delay: Long?,
        vararg filingPolicy: FilingPolicy,
        callback: RequestCallback?
    ) {
        client = mockableHttpClient(CIO) {
            mock {
                callback?.let { useDynamicMocks(it) }
                filingPolicy.forEach { decodeScenarioPathWith(it) }
                loadFileWith(loadingLambda)
                parseScenariosWith(mapper)
                delay?.let { addFakeNetworkDelay(it) }
                setInterceptorStatus(mode)
            }
            expectSuccess = false
            followRedirects = false
        }
    }

    override suspend fun assertResponseBody(expected: String, response: HttpResponse) {
        assertEquals(expected, response.readText())
    }

    override suspend fun assertResponseBodyStartsWith(expected: String, response: HttpResponse) {
        MatcherAssert.assertThat(
            response.readText(),
            StringStartsWith(
                expected
            )
        )
    }

    override fun assertResponseCode(resultCode: HttpStatusCode, response: HttpResponse) {
        assertEquals(resultCode, response.status)
    }

    override fun assertHeaderEquals(expected: String, response: HttpResponse, header: String) {
        assertEquals(expected, response.headers[header])
    }

    override fun assertContentType(type: String, subtype: String, response: HttpResponse) {
        assertEquals("application", response.contentType()?.contentType)
        assertEquals("json", response.contentType()?.contentSubtype)
    }
}
