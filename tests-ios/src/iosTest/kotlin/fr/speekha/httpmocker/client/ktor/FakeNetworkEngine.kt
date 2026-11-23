/*
 * Copyright 2019-2026 David Blanc
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

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlin.concurrent.AtomicInt

/**
 * Stands in for the real network as HttpMocker's delegate engine, so pass-through modes can be
 * tested deterministically, without any connectivity.
 *
 * Every request is answered with status 200, a `text/plain` body equal to [bodyPrefix] followed by
 * the request path, and an `X-Fake-Network: true` header.
 */
class FakeNetwork(private val bodyPrefix: String = "network:") : HttpClientEngineFactory<HttpClientEngineConfig> {

    private val counter = AtomicInt(0)

    /** Number of requests that reached this fake network. */
    val callCount: Int
        get() = counter.value

    override fun create(block: HttpClientEngineConfig.() -> Unit): HttpClientEngine =
        FakeNetworkEngine(HttpClientEngineConfig().apply(block))

    fun bodyFor(path: String) = "$bodyPrefix$path"

    private inner class FakeNetworkEngine(
        override val config: HttpClientEngineConfig
    ) : HttpClientEngineBase("fake-network") {

        override val dispatcher: CoroutineDispatcher = Dispatchers.IO

        @InternalAPI
        override suspend fun execute(data: HttpRequestData): HttpResponseData {
            counter.incrementAndGet()
            return HttpResponseData(
                statusCode = HttpStatusCode.OK,
                requestTime = GMTDate(),
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("text/plain"),
                    FAKE_NETWORK_HEADER to listOf("true")
                ),
                version = HttpProtocolVersion.HTTP_1_1,
                body = ByteReadChannel(bodyFor(data.url.encodedPath)),
                callContext = Job() + dispatcher
            )
        }
    }

    companion object {
        const val FAKE_NETWORK_HEADER = "X-Fake-Network"
    }
}
