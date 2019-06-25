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

package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class DynamicPolicy(
    private val mapper: Mapper,
    private val computeResponse: (Request) -> ResponseDescriptor
) : FilingPolicy {

    private val responses: MutableMap<String, ResponseDescriptor> = mutableMapOf()

    private val body: MutableMap<String, ByteArray> = mutableMapOf()

    override fun getPath(request: Request): String = computeKey(request).also { key ->
            val result = computeResponse(request)
            responses[key] = result.copy(body = "", bodyFile = "$key-body")
            body[key] = result.body.toByteArray()
        }

    private fun computeKey(request: Request): String = request.url()
            .toString()
            .replace('/', '_')

    fun loadScenario(key: String): InputStream? = if (key.endsWith("-body")) {
        body[key.dropLast(5)]?.let {
            ByteArrayInputStream(it)
        }
    } else {
        responses[key]?.let {
            PipedInputStream().apply {
                val pipeOut = PipedOutputStream()
                pipeOut.connect(this)
                mapper.writeValue(pipeOut, listOf(Matcher(response = it)))
            }
        }
    }
}
