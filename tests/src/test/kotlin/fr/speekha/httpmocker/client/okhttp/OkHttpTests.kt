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

import fr.speekha.httpmocker.HTTP_METHOD_GET
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.builder.FileLoader
import fr.speekha.httpmocker.client.HttpClientTester
import fr.speekha.httpmocker.client.TestWithServer
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.okhttp.MockResponseInterceptor
import fr.speekha.httpmocker.okhttp.builder.mockInterceptor
import fr.speekha.httpmocker.okhttp.builder.recordScenariosIn
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.MirrorPathPolicy
import fr.speekha.httpmocker.scenario.RequestCallback
import fr.speekha.httpmocker.serialization.Mapper
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.hamcrest.MatcherAssert
import org.hamcrest.core.StringStartsWith
import org.junit.jupiter.api.Assertions.assertEquals

open class OkHttpTests : TestWithServer(), HttpClientTester<Response, OkHttpClient> {

    protected lateinit var interceptor: MockResponseInterceptor

    override lateinit var client: OkHttpClient

    override val extraHeaders: List<Header> = emptyList()

    override fun changeMockerStatus(mode: Mode) {
        interceptor.mode = mode
    }

    override fun setupDynamicConf(vararg callbacks: RequestCallback, status: Mode): OkHttpClient = buildClient {
        mockInterceptor {
            setInterceptorStatus(status)
            callbacks.forEach {
                useDynamicMocks(it)
            }
        }
    }

    override fun setupStaticConf(
        mode: Mode,
        loadingLambda: FileLoader,
        mapper: Mapper,
        delay: Long?,
        vararg filingPolicy: FilingPolicy,
        callback: RequestCallback?
    ) {
        buildClient {
            mockInterceptor {
                callback?.let { useDynamicMocks(it) }
                filingPolicy.forEach { decodeScenarioPathWith(it) }
                loadFileWith(loadingLambda)
                parseScenariosWith(mapper)
                delay?.let { addFakeNetworkDelay(it) }
                setInterceptorStatus(mode)
            }
        }
    }

    override fun setupRecordConf(
        mapper: Mapper,
        loadingLambda: FileLoader,
        rootFolder: String,
        failOnError: Boolean,
        fileType: String
    ) {
        buildClient {
            mockInterceptor {
                decodeScenarioPathWith {
                    val path = it.path
                    (path + if (path.endsWith("/")) "index.$fileType" else ".$fileType")
                        .drop(1)
                }
                loadFileWith(loadingLambda)
                parseScenariosWith(mapper)
                recordScenariosIn(rootFolder) with MirrorPathPolicy(fileType)
                failOnRecordingError(failOnError)
                setInterceptorStatus(Mode.RECORD)
            }
        }
    }

    override fun setupRecordPolicyConf(mapper: Mapper, readPolicy: FilingPolicy?, writePolicy: FilingPolicy?) {
        buildClient {
            mockInterceptor {
                readPolicy?.let { decodeScenarioPathWith(it) }
                parseScenariosWith(mapper)
                writePolicy?.let {
                    recordScenariosIn(fr.speekha.httpmocker.client.SAVE_FOLDER) with it
                } ?: recordScenariosIn(fr.speekha.httpmocker.client.SAVE_FOLDER)
                failOnRecordingError(true)
                setInterceptorStatus(Mode.RECORD)
            }
        }
    }

    private fun buildClient(conf: () -> MockResponseInterceptor): OkHttpClient {
        interceptor = conf()
        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        return client
    }

    override suspend fun executeRequest(
        url: String,
        method: String,
        body: String?,
        headers: List<Pair<String, String>>
    ): Response {
        val request = buildRequest(completeLocalUrl(url), headers, method, body)
        return client.newCall(request).run()
    }

    private suspend fun Call.run() = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        execute()
    }

    @JvmOverloads
    fun executeRequestSync(
        url: String,
        method: String = HTTP_METHOD_GET,
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ): Response = runBlocking {
        executeRequest(url, method, body, headers)
    }

    private fun buildRequest(
        url: String,
        headers: List<Pair<String, String>> = emptyList(),
        method: String = HTTP_METHOD_GET,
        body: String? = null
    ): Request = Request.Builder()
        .url(url)
        .headers(Headers.headersOf(*headers.flatMap { listOf(it.first, it.second) }.toTypedArray()))
        .method(method, body?.toRequestBody("text/plain".toMediaTypeOrNull()))
        .build()

    override suspend fun checkResponseBody(
        expected: String,
        url: String,
        method: String,
        body: String?,
        headers: List<Pair<String, String>>
    ) {
        val response = executeRequest(url, method, body, headers)
        assertResponseCode(HttpStatusCode.OK, response)
        assertEquals(expected, response.readText())
    }

    override suspend fun assertResponseBody(expected: String, response: Response) {
        assertEquals(expected, response.readText())
    }

    override suspend fun assertResponseBodyStartsWith(expected: String, response: Response) {
        MatcherAssert.assertThat(
            response.readText(),
            StringStartsWith(
                expected
            )
        )
    }

    private suspend fun Response.readText() = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        body?.string()
    }

    override fun assertResponseCode(resultCode: HttpStatusCode, response: Response) {
        assertEquals(resultCode.value, response.code)
        assertEquals(resultCode.description, response.message)
    }

    override fun assertHeaderEquals(expected: String, response: Response, header: String) {
        assertEquals(expected, response.header(header))
    }

    override fun assertContentType(type: String, subtype: String, response: Response) {
        assertEquals("application", response.body?.contentType()?.type)
        assertEquals("json", response.body?.contentType()?.subtype)
        assertEquals("application/json", response.header("Content-type"))
    }
}
