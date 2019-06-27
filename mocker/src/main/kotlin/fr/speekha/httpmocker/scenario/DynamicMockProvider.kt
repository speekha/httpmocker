/*
 * Copyright 2019 David Blanc
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

package fr.speekha.httpmocker.scenario

import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.Request

internal class DynamicMockProvider(
    private val callback: RequestCallback
) : ScenarioProvider {

    private val body: MutableMap<Request, ByteArray> = mutableMapOf()

    override fun onRequest(request: Request): ResponseDescriptor? =
        callback.onRequest(request)?.let { result ->
            body[request] = result.body.toByteArray()
            result.copy(body = "", bodyFile = computeKey(request))
        }

    override fun loadResponseBody(request: Request, path: String): ByteArray? = body[request]

    private fun computeKey(request: Request): String = request.hashCode().toString()

}
