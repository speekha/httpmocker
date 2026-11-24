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

package fr.speekha.httpmocker.demo.scenarios

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.scenario.RequestCallback
import kotlinx.coroutines.CancellationException

private const val DEMO_HOST = "http://demo.example.com"
private const val GITHUB_API = "https://api.github.com"
private const val GET = "GET"
private const val POST = "POST"

data class ScenarioResult(
    val scenario: String,
    val url: String,
    val method: String,
    val statusCode: Int,
    val body: String,
    val error: String = ""
) {
    val isError: Boolean
        get() = statusCode == 0
}

/**
 * Same scenarios as the iOS demo (`MockDemoRunner`), run against the engine of the current flavor.
 *
 * @param clientFactory builds a client wrapped by HttpMocker in the given mode, answering with
 * the given dynamic mock
 */
class ScenarioRunner(
    private val clientFactory: (Mode, RequestCallback) -> ScenarioClient
) {

    suspend fun runAll(onResult: (ScenarioResult) -> Unit) {
        listOf(
            ::runEnabledMode,
            ::runMultipleRoutes,
            ::runCustomHeaders,
            ::runStatusCodes,
            ::runDisabledMode,
            ::runMixedMode
        ).forEach { scenario ->
            scenario().forEach(onResult)
        }
    }

    private suspend fun runEnabledMode(): List<ScenarioResult> = withClient(Mode.ENABLED, { request ->
        if (request.path == "/hello") ResponseDescriptor(code = 200, body = "Hello from HttpMocker!") else null
    }) {
        listOf(run("ENABLED mode – single route", GET, "$DEMO_HOST/hello"))
    }

    private suspend fun runMultipleRoutes(): List<ScenarioResult> = withClient(Mode.ENABLED, { request ->
        when {
            request.method == GET && request.path == "/users" ->
                ResponseDescriptor(code = 200, body = """[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]""")
            request.method == GET && request.path == "/posts" ->
                ResponseDescriptor(code = 200, body = """[{"id":1,"title":"First post"}]""")
            request.method == POST && request.path == "/users" ->
                ResponseDescriptor(code = 201, body = """{"id":3,"name":"Charlie"}""")
            else ->
                ResponseDescriptor(code = 404, body = "Not found")
        }
    }) {
        listOf(
            run("Multiple routes – GET /users", GET, "$DEMO_HOST/users"),
            run("Multiple routes – GET /posts", GET, "$DEMO_HOST/posts"),
            run("Multiple routes – POST /users", POST, "$DEMO_HOST/users")
        )
    }

    private suspend fun runCustomHeaders(): List<ScenarioResult> = withClient(Mode.ENABLED, {
        ResponseDescriptor(
            code = 200,
            body = """{"data":"secure"}""",
            mediaType = "application/json",
            headers = listOf(
                NamedParameter("X-Custom-Header", "demo-value"),
                NamedParameter("X-Request-Id", "abc-123")
            )
        )
    }) {
        listOf(run("Custom headers", GET, "$DEMO_HOST/secure"))
    }

    private suspend fun runStatusCodes(): List<ScenarioResult> = withClient(Mode.ENABLED, { request ->
        when (request.path) {
            "/ok" -> ResponseDescriptor(code = 200, body = "OK")
            "/created" -> ResponseDescriptor(code = 201, body = "Created")
            "/bad-request" -> ResponseDescriptor(code = 400, body = "Bad Request")
            "/not-found" -> ResponseDescriptor(code = 404, body = "Not Found")
            "/server-error" -> ResponseDescriptor(code = 500, body = "Internal Server Error")
            else -> null
        }
    }) {
        listOf("/ok", "/created", "/bad-request", "/not-found", "/server-error").map { path ->
            run("Status code – $path", GET, "$DEMO_HOST$path")
        }
    }

    private suspend fun runDisabledMode(): List<ScenarioResult> = withClient(Mode.DISABLED, {
        ResponseDescriptor(code = 200, body = "This should NOT appear")
    }) {
        listOf(run("DISABLED mode – mock bypassed (real call)", GET, "$GITHUB_API/zen"))
    }

    private suspend fun runMixedMode(): List<ScenarioResult> = withClient(Mode.MIXED, { request ->
        if (request.path == "/known") ResponseDescriptor(code = 200, body = "Known path – mocked response") else null
    }) {
        listOf(
            run("MIXED mode – known path (mocked)", GET, "$GITHUB_API/known"),
            run("MIXED mode – unknown path (real call)", GET, "$GITHUB_API/zen")
        )
    }

    private suspend fun withClient(
        mode: Mode,
        callback: RequestCallback,
        block: suspend ScenarioClient.() -> List<ScenarioResult>
    ): List<ScenarioResult> = clientFactory(mode, callback).use { it.block() }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun ScenarioClient.run(scenario: String, method: String, url: String): ScenarioResult = try {
        val response = call(method, url)
        ScenarioResult(scenario, url, method, response.statusCode, response.body)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ScenarioResult(scenario, url, method, 0, "", e.message ?: e::class.simpleName ?: "Unknown error")
    }
}
