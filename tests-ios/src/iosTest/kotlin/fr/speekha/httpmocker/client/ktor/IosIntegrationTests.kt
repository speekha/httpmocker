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
import fr.speekha.httpmocker.client.ktor.FakeNetworkEngine.Companion.FAKE_NETWORK_HEADER
import fr.speekha.httpmocker.io.FileAccessor
import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.io.IOException
import fr.speekha.httpmocker.io.withTemporaryFolder
import fr.speekha.httpmocker.io.writeText
import fr.speekha.httpmocker.kotlinx.KotlinxMapper
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.MirrorPathPolicy
import fr.speekha.httpmocker.scenario.RequestCallback
import fr.speekha.httpmocker.serialization.JSON_FORMAT
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val BASE_URL = "http://example.com"

class IosIntegrationTests {

    private val network = FakeNetworkEngine()

    // -------------------------------------------------------------------------
    // Static scenarios loaded from a real folder
    // -------------------------------------------------------------------------

    @Test
    fun `static scenario is loaded from a folder and answers the request`() = withTemporaryFolder("static") { folder ->
        folder.getFile("users").mkdir()
        folder.getFile("users/list.json").writeText(
            """[ { "response": { "code": 200, "media-type": "application/json", "body": "[\"Alice\",\"Bob\"]" } } ]"""
        )

        staticClient(folder).use { client ->
            runBlocking {
                val response = client.get("$BASE_URL/users/list")

                assertEquals(200, response.status.value)
                assertEquals("[\"Alice\",\"Bob\"]", response.bodyAsText())
                assertEquals("application/json", response.headers["Content-Type"])
            }
        }
        assertEquals(0, network.callCount)
    }

    @Test
    fun `static scenario body can be read from a separate body file`() = withTemporaryFolder("body-file") { folder ->
        folder.getFile("data.json").writeText(
            """[ { "response": { "media-type": "text/plain", "body-file": "data_body.txt" } } ]"""
        )
        folder.getFile("data_body.txt").writeText("Body from file – ünïcödé")

        staticClient(folder).use { client ->
            runBlocking {
                assertEquals("Body from file – ünïcödé", client.get("$BASE_URL/data").bodyAsText())
            }
        }
    }

    @Test
    fun `missing static scenario returns 404 in ENABLED mode`() = withTemporaryFolder("missing") { folder ->
        staticClient(folder).use { client ->
            runBlocking {
                assertEquals(404, client.get("$BASE_URL/unknown").status.value)
            }
        }
        assertEquals(0, network.callCount)
    }

    // -------------------------------------------------------------------------
    // Pass-through modes, using the fake network as delegate
    // -------------------------------------------------------------------------

    @Test
    fun `DISABLED mode ignores mocks and calls the network`() {
        dynamicClient(Mode.DISABLED) { ResponseDescriptor(code = 200, body = "mocked") }.use { client ->
            runBlocking {
                val response = client.get("$BASE_URL/path")

                assertEquals(network.bodyFor("/path"), response.bodyAsText())
                assertEquals("true", response.headers[FAKE_NETWORK_HEADER])
            }
        }
        assertEquals(1, network.callCount)
    }

    @Test
    fun `MIXED mode answers with a mock when one matches`() {
        dynamicClient(Mode.MIXED, ::mockKnownPath).use { client ->
            runBlocking {
                val response = client.get("$BASE_URL/known")

                assertEquals("mocked", response.bodyAsText())
                assertNull(response.headers[FAKE_NETWORK_HEADER])
            }
        }
        assertEquals(0, network.callCount)
    }

    @Test
    fun `MIXED mode falls back to the network when no mock matches`() {
        dynamicClient(Mode.MIXED, ::mockKnownPath).use { client ->
            runBlocking {
                val response = client.get("$BASE_URL/unknown")

                assertEquals(network.bodyFor("/unknown"), response.bodyAsText())
                assertEquals("true", response.headers[FAKE_NETWORK_HEADER])
            }
        }
        assertEquals(1, network.callCount)
    }

    @Test
    fun `ENABLED mode never calls the network`() {
        dynamicClient(Mode.ENABLED, ::mockKnownPath).use { client ->
            runBlocking {
                assertEquals(404, client.get("$BASE_URL/unknown").status.value)
            }
        }
        assertEquals(0, network.callCount)
    }

    // -------------------------------------------------------------------------
    // RECORD mode writing into a real folder
    // -------------------------------------------------------------------------

    @Test
    fun `RECORD mode returns the network response and saves nested scenario files`() =
        withTemporaryFolder("record") { folder ->
            recordingClient(folder).use { client ->
                runBlocking {
                    val response = client.get("$BASE_URL/api/v1/users")

                    assertEquals(network.bodyFor("/api/v1/users"), response.bodyAsText())
                }
            }

            val scenario = folder.getFile("api/v1/users.json")
            val body = folder.getFile("api/v1/users_body_0.txt")
            assertTrue(scenario.exists(), "Scenario file should be saved at ${scenario.absolutePath}")
            assertTrue(body.exists(), "Body file should be saved at ${body.absolutePath}")
            assertEquals(network.bodyFor("/api/v1/users"), body.getReader().readAsString())
            assertTrue(scenario.getReader().readAsString().contains("users_body_0.txt"))
        }

    @Test
    fun `recorded scenarios can be replayed without the network`() = withTemporaryFolder("replay") { folder ->
        recordingClient(folder).use { client ->
            runBlocking { client.get("$BASE_URL/api/items") }
        }
        assertEquals(1, network.callCount)

        staticClient(folder).use { client ->
            runBlocking {
                assertEquals(network.bodyFor("/api/items"), client.get("$BASE_URL/api/items").bodyAsText())
            }
        }
        assertEquals(1, network.callCount)
    }

    @Test
    fun `recording the same request twice appends a second scenario`() = withTemporaryFolder("append") { folder ->
        recordingClient(folder).use { client ->
            runBlocking {
                client.get("$BASE_URL/repeated")
                client.get("$BASE_URL/repeated")
            }
        }

        assertTrue(folder.getFile("repeated_body_0.txt").exists())
        assertTrue(folder.getFile("repeated_body_1.txt").exists())
    }

    @Test
    fun `recording failure is reported when failOnRecordingError is enabled`() =
        withTemporaryFolder("record-failure") { folder ->
            folder.getFile("blocked").writeText("a file where a folder is needed")

            recordingClient(folder, failOnError = true).use { client ->
                assertFailsWith<IOException> {
                    runBlocking { client.get("$BASE_URL/blocked/resource") }
                }
            }
        }

    @Test
    fun `recording failure is ignored when failOnRecordingError is disabled`() =
        withTemporaryFolder("record-failure-ignored") { folder ->
            folder.getFile("blocked").writeText("a file where a folder is needed")

            recordingClient(folder, failOnError = false).use { client ->
                runBlocking {
                    val response = client.get("$BASE_URL/blocked/resource")

                    assertEquals(network.bodyFor("/blocked/resource"), response.bodyAsText())
                }
            }
            assertFalse(folder.getFile("blocked/resource.json").exists())
        }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun mockKnownPath(request: HttpRequest) =
        if (request.path == "/known") ResponseDescriptor(code = 200, body = "mocked") else null

    private fun dynamicClient(mode: Mode, callback: RequestCallback): HttpClient =
        mockableHttpClient(network) {
            mock {
                setMode(mode)
                useDynamicMocks(callback)
            }
            expectSuccess = false
        }

    private fun staticClient(folder: FileAccessor): HttpClient =
        mockableHttpClient(network) {
            mock {
                setMode(Mode.ENABLED)
                decodeScenarioPathWith(MirrorPathPolicy(JSON_FORMAT))
                loadFileWith { path -> folder.getFile(path).takeIf { it.exists() }?.getReader() }
                parseScenariosWith(KotlinxMapper())
            }
            expectSuccess = false
        }

    private fun recordingClient(folder: FileAccessor, failOnError: Boolean = true): HttpClient =
        mockableHttpClient(network) {
            mock {
                parseScenariosWith(KotlinxMapper())
                recordScenariosIn(folder)
                failOnRecordingError(failOnError)
                setMode(Mode.RECORD)
            }
            expectSuccess = false
        }
}
