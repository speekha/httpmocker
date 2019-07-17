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

package fr.speekha.httpmocker.interceptor

import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.MockResponseInterceptor.Mode.ENABLED
import fr.speekha.httpmocker.MockResponseInterceptor.Mode.RECORD
import fr.speekha.httpmocker.NO_RECORDER_ERROR
import fr.speekha.httpmocker.NO_ROOT_FOLDER_ERROR
import fr.speekha.httpmocker.buildRequest
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.scenario.RequestCallback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DynamicMockTests {

    private lateinit var interceptor: MockResponseInterceptor

    private lateinit var client: OkHttpClient

    @Test
    fun `should reply with a dynamically generated response`() {
        setupProvider {
            ResponseDescriptor(code = 202, body = "some random body")
        }
        val response = client.newCall(
            buildRequest(
                url,
                method = "GET"
            )
        ).execute()

        assertEquals(202, response.code())
        assertEquals("some random body", response.body()?.string())
    }

    @Test
    fun `should reply with a stateful callback`() {
        val body = "Time: ${System.currentTimeMillis()}"
        val callback = object : RequestCallback {
            override fun loadResponse(request: Request) =
                ResponseDescriptor(code = 202, body = body)
        }
        setupProvider(callback)

        val response = client.newCall(
            buildRequest(
                url,
                method = "GET"
            )
        ).execute()

        assertEquals(202, response.code())
        assertEquals(body, response.body()?.string())
    }

    @Test
    fun `should support multiple callbacks`() {
        val result1 = "First mock"
        val result2 = "Second mock"

        interceptor = MockResponseInterceptor.Builder()
            .useDynamicMocks {
                if (it.url().toString().contains("1"))
                    ResponseDescriptor(body = result1)
                else null
            }.useDynamicMocks {
                ResponseDescriptor(body = result2)
            }
            .setInterceptorStatus(ENABLED)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val response1 =
            client.newCall(
                buildRequest(
                    "http://www.test.fr/request1",
                    method = "GET"
                )
            ).execute()
        val response2 =
            client.newCall(
                buildRequest(
                    "http://www.test.fr/request2",
                    method = "GET"
                )
            ).execute()

        assertEquals(result1, response1.body()?.string())
        assertEquals(result2, response2.body()?.string())
    }

    @Test
    fun `should not allow init an interceptor in record mode with no recorder`() {
        val exception = assertThrows<IllegalStateException> { setupProvider(RECORD) { null } }
        assertEquals(NO_ROOT_FOLDER_ERROR, exception.message)
        assertFalse(::interceptor.isInitialized)
    }

    @Test
    fun `should not allow to record requests if recorder is not set`() {
        setupProvider { null }
        val exception = assertThrows<IllegalStateException> {
            interceptor.mode = RECORD
        }
        assertEquals(NO_RECORDER_ERROR, exception.message)
    }

    private fun setupProvider(callback: RequestCallback) {
        interceptor = MockResponseInterceptor.Builder()
            .useDynamicMocks(callback)
            .setInterceptorStatus(ENABLED)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    }

    private fun setupProvider(
        status: MockResponseInterceptor.Mode = ENABLED,
        callback: (Request) -> ResponseDescriptor?
    ) {
        interceptor = MockResponseInterceptor.Builder()
            .useDynamicMocks(callback)
            .setInterceptorStatus(status)
            .build()

        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    companion object {
        const val url = "http://www.test.fr/path1?param=1"
    }
}