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

package fr.speekha.httpmocker.demo

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.scenario.RequestCallback
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val GITHUB_API = "https://api.github.com"

/**
 * Result of a single demo HTTP call, exposed to Swift.
 */
data class DemoResult(
    val scenario: String,
    val url: String,
    val method: String,
    val statusCode: Int,
    val body: String,
    val error: String = ""
)

/**
 * Callback interface for Swift to receive results asynchronously.
 */
interface DemoResultCallback {
    fun onResult(result: DemoResult)
    fun onAllDone()
}

/**
 * High-level runner that exercises several HttpMocker features and reports results to Swift.
 *
 * Usage from Swift:
 * ```swift
 * let runner = MockDemoRunner()
 * runner.runAllScenarios(callback: myCallback)
 * ```
 */
class MockDemoRunner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Run all built-in demo scenarios in sequence and deliver results via [callback]. */
    fun runAllScenarios(callback: DemoResultCallback) {
        scope.launch {
            val results = mutableListOf<DemoResult>()

            results += runEnabledMode()
            results += runMultipleRoutes()
            results += runCustomHeaders()
            results += runStatusCodes()
            results += runDisabledMode()
            results += runMixedMode()

            results.forEach { callback.onResult(it) }
            callback.onAllDone()
        }
    }

    // ---------------------------------------------------------------------------
    // Scenario 1 – basic ENABLED mode with a single callback
    // ---------------------------------------------------------------------------
    private suspend fun runEnabledMode(): DemoResult {
        val client = buildClient(Mode.ENABLED) { request ->
            if (request.path == "/hello") {
                ResponseDescriptor(code = 200, body = "Hello from HttpMocker!")
            } else {
                null
            }
        }
        return client.use {
            runRequest(
                client = it,
                scenario = "ENABLED mode – single route",
                url = "http://demo.example.com/hello",
                method = "GET"
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Scenario 2 – multiple routes in a single callback
    // ---------------------------------------------------------------------------
    private suspend fun runMultipleRoutes(): List<DemoResult> {
        val client = buildClient(Mode.ENABLED) { request ->
            when {
                request.method == "GET" && request.path == "/users" ->
                    ResponseDescriptor(code = 200, body = """[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]""")
                request.method == "GET" && request.path == "/posts" ->
                    ResponseDescriptor(code = 200, body = """[{"id":1,"title":"First post"}]""")
                request.method == "POST" && request.path == "/users" ->
                    ResponseDescriptor(code = 201, body = """{"id":3,"name":"Charlie"}""")
                else ->
                    ResponseDescriptor(code = 404, body = "Not found")
            }
        }

        return client.use {
            listOf(
                runRequest(it, "Multiple routes – GET /users", "http://demo.example.com/users", "GET"),
                runRequest(it, "Multiple routes – GET /posts", "http://demo.example.com/posts", "GET"),
                runRequest(it, "Multiple routes – POST /users", "http://demo.example.com/users", "POST")
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Scenario 3 – custom response headers
    // ---------------------------------------------------------------------------
    private suspend fun runCustomHeaders(): DemoResult {
        val client = buildClient(Mode.ENABLED) { _ ->
            ResponseDescriptor(
                code = 200,
                body = """{"data":"secure"}""",
                mediaType = "application/json",
                headers = listOf(
                    NamedParameter("X-Custom-Header", "demo-value"),
                    NamedParameter("X-Request-Id", "abc-123")
                )
            )
        }
        return client.use { runRequest(it, "Custom headers", "http://demo.example.com/secure", "GET") }
    }

    // ---------------------------------------------------------------------------
    // Scenario 4 – various HTTP status codes
    // ---------------------------------------------------------------------------
    private suspend fun runStatusCodes(): List<DemoResult> {
        val client = buildClient(Mode.ENABLED) { request ->
            when (request.path) {
                "/ok" -> ResponseDescriptor(code = 200, body = "OK")
                "/created" -> ResponseDescriptor(code = 201, body = "Created")
                "/bad-request" -> ResponseDescriptor(code = 400, body = "Bad Request")
                "/not-found" -> ResponseDescriptor(code = 404, body = "Not Found")
                "/server-error" -> ResponseDescriptor(code = 500, body = "Internal Server Error")
                else -> null
            }
        }

        return client.use {
            listOf("/ok", "/created", "/bad-request", "/not-found", "/server-error").map { path ->
                runRequest(it, "Status code – $path", "http://demo.example.com$path", "GET")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Scenario 5 – DISABLED mode (mocks ignored, real GitHub API called)
    // ---------------------------------------------------------------------------
    private suspend fun runDisabledMode(): DemoResult {
        val client = buildClient(Mode.DISABLED) { _ ->
            ResponseDescriptor(code = 200, body = "This should NOT appear")
        }
        return client.use {
            runRequest(it, "DISABLED mode – mock bypassed (real call)", "$GITHUB_API/zen", "GET")
        }
    }

    // ---------------------------------------------------------------------------
    // Scenario 6 – MIXED mode (mock answers when callback returns non-null)
    // ---------------------------------------------------------------------------
    private suspend fun runMixedMode(): List<DemoResult> {
        val client = buildClient(Mode.MIXED) { request ->
            if (request.path == "/known") {
                ResponseDescriptor(code = 200, body = "Known path – mocked response")
            } else {
                null
            }
        }

        return client.use {
            listOf(
                runRequest(it, "MIXED mode – known path (mocked)", "$GITHUB_API/known", "GET"),
                runRequest(it, "MIXED mode – unknown path (real call)", "$GITHUB_API/zen", "GET")
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun buildClient(mode: Mode, callback: RequestCallback): HttpClient =
        mockableHttpClient(Darwin) {
            mock {
                setMode(mode)
                useDynamicMocks(callback)
            }
            expectSuccess = false
            followRedirects = false
        }

    private suspend fun runRequest(
        client: HttpClient,
        scenario: String,
        url: String,
        method: String
    ): DemoResult =
        try {
            val response = when (method) {
                "POST" -> client.post(url) { setBody("") }
                else -> client.get(url)
            }
            DemoResult(
                scenario = scenario,
                url = url,
                method = method,
                statusCode = response.status.value,
                body = response.bodyAsText()
            )
        } catch (e: Exception) {
            DemoResult(
                scenario = scenario,
                url = url,
                method = method,
                statusCode = 0,
                body = "",
                error = e.message ?: "Unknown error"
            )
        }
}
