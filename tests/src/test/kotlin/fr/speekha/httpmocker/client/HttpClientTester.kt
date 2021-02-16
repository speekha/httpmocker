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

package fr.speekha.httpmocker.client

import fr.speekha.httpmocker.HTTP_METHOD_GET
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.builder.FileLoader
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.scenario.RequestCallback
import fr.speekha.httpmocker.serialization.Mapper
import io.ktor.http.*

interface HttpClientTester<Response : Any, Client : Any> {

    var client: Client

    val extraHeaders: List<NamedParameter>

    fun setupDynamicConf(
        vararg callbacks: RequestCallback,
        mode: Mode = Mode.ENABLED
    ): Client

    fun setupStaticConf(
        mode: Mode,
        loadingLambda: FileLoader,
        mapper: Mapper,
        delay: Long? = null,
        vararg filingPolicy: FilingPolicy,
        callback: RequestCallback? = null
    )

    fun setupRecordConf(
        mapper: Mapper,
        loadingLambda: FileLoader,
        rootFolder: String = SAVE_FOLDER,
        failOnError: Boolean = false,
        fileType: String
    )

    fun setupRecordPolicyConf(mapper: Mapper, readPolicy: FilingPolicy?, writePolicy: FilingPolicy?)

    fun enqueueServerResponse(
        responseCode: Int,
        responseBody: String?,
        headers: List<Pair<String, String>> = listOf(),
        contentType: String? = null
    )

    fun changeMockerStatus(mode: Mode)

    suspend fun executeRequest(
        url: String,
        method: String = HTTP_METHOD_GET,
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ): Response

    suspend fun check404Response(
        url: String,
        method: String = HTTP_METHOD_GET,
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    ) {
        assertResponseCode(
            HttpStatusCode.NotFound,
            executeRequest(url, method, body, headers)
        )
    }

    suspend fun checkResponseBody(
        expected: String,
        url: String,
        method: String = HTTP_METHOD_GET,
        body: String? = null,
        headers: List<Pair<String, String>> = emptyList()
    )

    fun completeLocalUrl(url: String): String

    suspend fun assertResponseBody(expected: String, response: Response)
    suspend fun assertResponseBodyStartsWith(expected: String, response: Response)
    fun assertResponseCode(resultCode: HttpStatusCode, response: Response)
    fun assertHeaderEquals(expected: String, response: Response, header: String)
    fun assertContentType(type: String, subtype: String, response: Response)
    fun assertFilesExist(vararg path: String)
}
