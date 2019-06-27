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

package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.buildRequest
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.scenario.RequestCallback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DynamicMockProviderTest {

    private lateinit var interceptor: MockResponseInterceptor

    private lateinit var client: OkHttpClient

    @Test
    fun `should reply with a dynamically generated response`() {
        setupProvider {
            ResponseDescriptor(code = 202, body = "some random body")
        }
        val response = client.newCall(buildRequest(url, method = "GET")).execute()

        Assertions.assertEquals(202, response.code())
        Assertions.assertEquals("some random body", response.body()?.string())
    }

    @Test
    fun `should reply with a stateful callback`() {
        val body = "Time: ${System.currentTimeMillis()}"
        val callback = object : RequestCallback {
            override fun onRequest(request: Request) = ResponseDescriptor(code = 202, body = body)
        }
        setupProvider(callback)

        val response = client.newCall(buildRequest(url, method = "GET")).execute()

        Assertions.assertEquals(202, response.code())
        Assertions.assertEquals(body, response.body()?.string())
    }

    private fun setupProvider(callback: RequestCallback) {
        interceptor = MockResponseInterceptor.Builder()
            .useDynamicMocks(callback)
            .setInterceptorStatus(MockResponseInterceptor.Mode.ENABLED)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    }

    private fun setupProvider(callback: (Request) -> ResponseDescriptor) {
        interceptor = MockResponseInterceptor.Builder()
            .useDynamicMocks(callback)
            .setInterceptorStatus(MockResponseInterceptor.Mode.ENABLED)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    companion object {
        const val url = "http://www.test.fr/path1?param=1"
    }
}