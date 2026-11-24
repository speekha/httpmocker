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
import fr.speekha.httpmocker.okhttp.builder.mockInterceptor
import fr.speekha.httpmocker.scenario.RequestCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

fun createScenarioClient(mode: Mode, callback: RequestCallback): ScenarioClient {
    val interceptor = mockInterceptor {
        useDynamicMocks(callback)
        setMode(mode)
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .followRedirects(false)
        .build()
    return OkHttpScenarioClient(client)
}

private class OkHttpScenarioClient(private val client: OkHttpClient) : ScenarioClient {

    override suspend fun call(method: String, url: String): ScenarioResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .method(method, if (method == "POST") "".toRequestBody() else null)
            .build()
        client.newCall(request).execute().use { response ->
            ScenarioResponse(response.code, response.body.string())
        }
    }

    override fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
