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
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.scenario.RequestCallback
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText

fun createScenarioClient(mode: Mode, callback: RequestCallback): ScenarioClient =
    KtorScenarioClient(
        mockableHttpClient(CIO) {
            mock {
                setMode(mode)
                useDynamicMocks(callback)
            }
            expectSuccess = false
            followRedirects = false
        }
    )

private class KtorScenarioClient(private val client: HttpClient) : ScenarioClient {

    override suspend fun call(method: String, url: String): ScenarioResponse {
        val response = when (method) {
            "POST" -> client.post(url) { setBody("") }
            else -> client.get(url)
        }
        return ScenarioResponse(response.status.value, response.bodyAsText())
    }

    override fun close() = client.close()
}
