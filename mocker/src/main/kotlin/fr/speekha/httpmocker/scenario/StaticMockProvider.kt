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

import fr.speekha.httpmocker.LoadFile
import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.matchBody
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import okhttp3.Request
import java.util.Locale

internal class StaticMockProvider(
    private val filingPolicy: FilingPolicy,
    private val loadFileContent: LoadFile,
    private val mapper: Mapper
) : ScenarioProvider {

    private val logger = getLogger()

    override fun loadResponse(request: Request): ResponseDescriptor? = try {
        val path = filingPolicy.getPath(request)
        logger.info("Loading scenarios from $path")
        loadFileContent(path)?.let { stream ->
            val list = mapper.readMatches(stream)
            matchRequest(request, list)
        }
    } catch (e: Throwable) {
        logger.error("Scenario file could not be loaded", e)
        null
    }

    private fun matchRequest(request: Request, list: List<Matcher>): ResponseDescriptor? =
        list.firstOrNull { it.request.match(request) }?.response
            .also { logger.info(if (it != null) "Match found" else "No match for request") }

    private fun RequestDescriptor.match(request: Request): Boolean =
        (method?.equals(request.method(), true) ?: true) &&
                (host?.equals(request.url().host(), true) ?: true) &&
                (port?.let { it == request.url().port() } ?: true) &&
                (path?.let { it == request.url().encodedPath() } ?: true) &&
                (host?.let { it.toLowerCase(Locale.ROOT) == request.url().host() } ?: true) &&
                (port?.let { it == request.url().port() } ?: true) &&
                (path?.let { it == request.url().encodedPath() } ?: true) &&
                headers.all { request.headers(it.name).contains(it.value) } &&
                params.all { request.url().queryParameter(it.key) == it.value } &&
                request.matchBody(this)

    override fun loadResponseBody(request: Request, path: String): ByteArray? =
        loadFileContent(getRelativePath(filingPolicy.getPath(request), path))?.readBytes()

    private fun getRelativePath(base: String, child: String): String =
        concatenatePaths(base, child).cleanFolderList().joinToString("/")

    private fun concatenatePaths(base: String, child: String) =
        base.split("/").dropLast(1) + child.split("/")

    private fun List<String>.cleanFolderList() = filterIndexed { index, segment ->
        segment != ".." && (index == size - 1 || get(index + 1) != "..")
    }

    override fun toString(): String = "static mock configuration"
}