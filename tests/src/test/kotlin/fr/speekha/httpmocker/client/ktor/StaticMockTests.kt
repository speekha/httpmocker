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

package fr.speekha.httpmocker.client.ktor

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.client.HttpClientTester
import fr.speekha.httpmocker.client.StaticMockTests
import fr.speekha.httpmocker.client.TestWithServer
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.serialization.Mapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("UNUSED_PARAMETER")
@DisplayName("Static Mocks with Ktor")
class StaticMockTests :
    StaticMockTests<HttpResponse, HttpClient>(),
    HttpClientTester<HttpResponse, HttpClient> by KtorTests() {

    @Nested
    @DisplayName("Given a disabled mock interceptor")
    inner class ChainedEngines {

        @ParameterizedTest(name = "Mapper: {0}")
        @MethodSource("fr.speekha.httpmocker.client.TestWithServer#mappers")
        @DisplayName(
            "When Ktor connectors are added to the client, " +
                "then those connectors should not be affected by HttpMocker"
        )
        fun `should allow to add connectors`(
            title: String,
            mapper: Mapper,
            type: String
        ) = runBlocking {
            initFilingPolicy(type)
            client = mockableHttpClient(CIO) {
                mock {
                    decodeScenarioPathWith(filingPolicy)
                    loadFileWith(loadingLambda)
                    parseScenariosWith(mapper)
                    setMode(Mode.DISABLED)
                }
                install(JsonFeature) {
                    serializer = KotlinxSerializer()
                }
                expectSuccess = false
                followRedirects = false
            }
            enqueueServerResponse(
                TestWithServer.REQUEST_OK_CODE,
                "{ \"field\":\"value\"}",
                contentType = "application/json"
            )

            val result: JsonObject = client.get(completeLocalUrl("/"))
            Assertions.assertEquals(JsonObject("value"), result)
        }
    }

    @Serializable
    data class JsonObject(
        @SerialName("field")
        val field: String?
    )
}
