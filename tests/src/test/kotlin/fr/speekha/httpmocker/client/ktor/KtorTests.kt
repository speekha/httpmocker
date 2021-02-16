/*
 * Copyright 2019-2021 David Blanc
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
import fr.speekha.httpmocker.client.SAVE_FOLDER
import fr.speekha.httpmocker.client.TestWithServer
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.ktor.engine.MockEngine
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.MirrorPathPolicy
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

class KtorTests : TestWithServer(), HttpClientTester<HttpResponse, HttpClient> {

    override lateinit var client: HttpClient

    override val extraHeaders: List<NamedParameter> = listOf(
        NamedParameter("Accept-Charset", "UTF-8"),
        NamedParameter("Accept", "*/*")
    )

    override fun changeMockerStatus(mode: Mode) {
        (client.engine as MockEngine).mode = mode
    }

    override fun setupDynamicConf(
        vararg callbacks: RequestCallback,
        mode: Mode
    ): HttpClient = mockableHttpClient(CIO) {
        mock {
            setMode(mode)
            callbacks.forEach {
                useDynamicMocks(it)
            }
        }
        expectSuccess = false
        followRedirects = false
    }.also { client = it }

    override fun setupStaticConf(
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
                setMode(mode)
            }
            expectSuccess = false
            followRedirects = false
        }
    }

    override fun setupRecordConf(
        mapper: Mapper,
        loadingLambda: FileLoader,
        rootFolder: String,
        failOnError: Boolean,
        fileType: String
    ) {
        client = mockableHttpClient(CIO) {
            mock {
                decodeScenarioPathWith {
                    val path = it.path
                    (path + if (path.endsWith("/")) "index.$fileType" else ".$fileType")
                        .drop(1)
                }
                loadFileWith(loadingLambda)
                parseScenariosWith(mapper)
                recordScenariosIn(rootFolder) with MirrorPathPolicy(fileType)
                failOnRecordingError(failOnError)
                setMode(Mode.RECORD)
            }
        }
    }

    override fun setupRecordPolicyConf(mapper: Mapper, readPolicy: FilingPolicy?, writePolicy: FilingPolicy?) {
        client = mockableHttpClient(CIO) {
            mock {
                readPolicy?.let { decodeScenarioPathWith(it) }
                parseScenariosWith(mapper)
                writePolicy?.let {
                    recordScenariosIn(SAVE_FOLDER) with it
                } ?: recordScenariosIn(SAVE_FOLDER)
                failOnRecordingError(true)
                setMode(Mode.RECORD)
            }
        }
    }

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
