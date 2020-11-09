/*
 * Copyright 2019-2020 David Blanc
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
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.okhttp.MockResponseInterceptor
import fr.speekha.httpmocker.okhttp.builder.mockInterceptor
import fr.speekha.httpmocker.policies.SingleFolderPolicy
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

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
        val interceptor = setupInterceptor(policy)
        val client = setupClient(interceptor)
        val retrofit = setupRetrofit(client)
        return retrofit.create(GithubApiEndpoints::class.java)
    }

    private fun setupInterceptor(policy: SingleFolderPolicy): MockResponseInterceptor =
        mockInterceptor {
            decodeScenarioPathWith(policy)
            loadFileWith { javaClass.classLoader?.getResourceAsStream(it) }
            parseScenariosWith(JacksonMapper())
            setInterceptorStatus(Mode.ENABLED)
        }

    private fun setupClient(interceptor: MockResponseInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

    private fun setupRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com")
        .client(client)
        .addConverterFactory(JacksonConverterFactory.create())
        .build()
}
