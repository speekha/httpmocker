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
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DynamicPolicyTest {

    private val mapper = JacksonMapper()

    private lateinit var policy: DynamicPolicy

    private lateinit var interceptor: MockResponseInterceptor

    private lateinit var client: OkHttpClient

    fun setupPolicy(aPolicy: DynamicPolicy) {
        policy = aPolicy

        interceptor = MockResponseInterceptor.Builder()
            .useDynamicMocks(policy)
            .parseScenariosWith(mapper)
            .setInterceptorStatus(MockResponseInterceptor.Mode.ENABLED)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    @Test
    fun `should reply with a dynamically generated response`() {
        setupPolicy(DynamicPolicy(mapper) {
            ResponseDescriptor(
                code = 202,
                body = "some random body"
            )
        })
        val response = client.newCall(buildRequest(url, method = "GET")).execute()

        Assertions.assertEquals(202, response.code())
        Assertions.assertEquals("some random body", response.body()?.string())
    }

    @Test
    fun `should reply with a stateful callback`() {
        val body = "Time: ${System.currentTimeMillis()}"
        val callback = object : DynamicPolicy.RequestCallback {
            override fun onRequest(request: Request) = ResponseDescriptor(
                code = 202,
                body = body
            )
        }
        setupPolicy(DynamicPolicy(mapper, callback))

        val response = client.newCall(buildRequest(url, method = "GET")).execute()

        Assertions.assertEquals(202, response.code())
        Assertions.assertEquals(body, response.body()?.string())
    }

    companion object {
        const val url = "http://www.test.fr/path1?param=1"
    }
}