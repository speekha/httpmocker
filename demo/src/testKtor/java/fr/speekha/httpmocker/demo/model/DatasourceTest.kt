/*
 * Copyright 2019-2021 David Blanc
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

package fr.speekha.httpmocker.demo.model

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import fr.speekha.httpmocker.demo.service.GithubEndpointWithKtor
import fr.speekha.httpmocker.io.asReader
import fr.speekha.httpmocker.kotlinx.KotlinxMapper
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.policies.SingleFolderPolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * These test cases demonstrate how to use HttpMocker to mock service calls in automated tests, so
 * you can test all your service call stack together easily.
 */
class DatasourceTest {

    @Test
    fun `should return an empty list`() {
        val service = setupService("test1")
        runBlocking {
            val repos = service.listRepositoriesForOrganisation("kotlin")
            assertEquals(emptyList<Repo>(), repos)
        }
    }

    @Test
    fun `should return a list with a single item`() {
        val service = setupService("test2")
        runBlocking {
            val repos = service.listRepositoriesForOrganisation("kotlin")
            assertEquals(listOf(Repo(8856204, "kotlin-examples")), repos)
        }
    }

    private fun setupService(folder: String): GithubApiEndpoints {
        val policy = SingleFolderPolicy(folder)
        val client = setupClient(policy)
        return GithubEndpointWithKtor(client)
    }

    private fun setupClient(policy: SingleFolderPolicy): HttpClient =
        mockableHttpClient(CIO) {
            mock {
                decodeScenarioPathWith(policy)
                loadFileWith { javaClass.classLoader?.getResourceAsStream(it)?.asReader() }
                parseScenariosWith(KotlinxMapper())
                setMode(Mode.ENABLED)
            }

            install(JsonFeature) {
                serializer = KotlinxSerializer(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }

            expectSuccess = false
            followRedirects = false
        }
}
