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

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.ktor.engine.MockEngine
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.scenario.RequestCallback
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorSampleTests {

    private lateinit var client: HttpClient

    private fun setupStaticConf(
        mode: Mode,
        vararg callbacks: RequestCallback
    ) {
        client = mockableHttpClient(CIO) {
            mock {
                setMode(mode)
                callbacks.forEach {
                    useDynamicMocks(it)
                }
            }
            expectSuccess = false
            followRedirects = false
        }
    }

    @Test
    fun `Ktor client with mock engine responds to GET requests`() = runBlocking {
        setupStaticConf(Mode.ENABLED, RequestCallback { request ->
            when {
                request.method == "GET" && request.path == "/api/test" ->
                    fr.speekha.httpmocker.model.ResponseDescriptor(
                        code = 200,
                        body = "mock response"
                    )

                else -> null
            }
        })

        try {
            val response: HttpResponse = client.get("http://example.com/api/test")
            assertEquals(200, response.status.value)
            assertEquals("mock response", response.bodyAsText())
        } finally {
            client.close()
        }
    }

    @Test
    fun `Ktor client handles multiple requests`() = runBlocking {
        setupStaticConf(Mode.ENABLED, RequestCallback { request ->
            when {
                request.method == "GET" && request.path == "/users" ->
                    fr.speekha.httpmocker.model.ResponseDescriptor(code = 200, body = "user list")

                request.method == "GET" && request.path == "/posts" ->
                    fr.speekha.httpmocker.model.ResponseDescriptor(code = 200, body = "post list")

                request.method == "POST" && request.path == "/users" ->
                    fr.speekha.httpmocker.model.ResponseDescriptor(code = 201, body = "created")

                else ->
                    fr.speekha.httpmocker.model.ResponseDescriptor(code = 404, body = "not found")
            }
        })

        try {
            val response1: HttpResponse = client.get("http://example.com/users")
            assertEquals(200, response1.status.value)
            assertEquals("user list", response1.bodyAsText())

            val response2: HttpResponse = client.get("http://example.com/posts")
            assertEquals(200, response2.status.value)
            assertEquals("post list", response2.bodyAsText())
        } finally {
            client.close()
        }
    }

    @Test
    fun `Ktor client respects response headers`() = runBlocking {
        setupStaticConf(Mode.ENABLED, RequestCallback { request ->
            fr.speekha.httpmocker.model.ResponseDescriptor(
                code = 200,
                body = "data",
                mediaType = "application/json",
                headers = listOf(
                    NamedParameter("X-Custom-Header", "custom-value")
                )
            )
        })

        try {
            val response: HttpResponse = client.get("http://example.com/data")
            assertEquals(200, response.status.value)
            assertEquals("data", response.bodyAsText())
            assertEquals("application/json", response.headers["Content-Type"])
            assertEquals("custom-value", response.headers["X-Custom-Header"])
        } finally {
            client.close()
        }
    }

    @Test
    fun `Ktor client respects response codes`() = runBlocking {
        setupStaticConf(Mode.ENABLED, RequestCallback { request ->
            when (request.path) {
                "/ok" -> fr.speekha.httpmocker.model.ResponseDescriptor(code = 200)
                "/created" -> fr.speekha.httpmocker.model.ResponseDescriptor(code = 201)
                "/bad-request" -> fr.speekha.httpmocker.model.ResponseDescriptor(code = 400)
                "/not-found" -> fr.speekha.httpmocker.model.ResponseDescriptor(code = 404)
                "/server-error" -> fr.speekha.httpmocker.model.ResponseDescriptor(code = 500)
                else -> null
            }
        })

        try {
            assertEquals(200, client.get("http://example.com/ok").status.value)
            assertEquals(201, client.get("http://example.com/created").status.value)
            assertEquals(400, client.get("http://example.com/bad-request").status.value)
            assertEquals(404, client.get("http://example.com/not-found").status.value)
            assertEquals(500, client.get("http://example.com/server-error").status.value)
        } finally {
            client.close()
        }
    }

    @Test
    fun `Ktor client can be disabled`() = runBlocking {
        setupStaticConf(Mode.DISABLED, RequestCallback { request ->
            fr.speekha.httpmocker.model.ResponseDescriptor(code = 200, body = "mocked")
        })

        val engine = client.engine as? MockEngine
        if (engine != null) {
            engine.mode = Mode.DISABLED
        }
        client.close()
    }
}
