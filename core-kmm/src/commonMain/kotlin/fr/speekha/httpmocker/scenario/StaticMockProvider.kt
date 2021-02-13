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

package fr.speekha.httpmocker.scenario

import fr.speekha.httpmocker.ClassloaderUtils
import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.io.IOException
import fr.speekha.httpmocker.io.StreamReader
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestResult
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.serialization.Mapper
import fr.speekha.httpmocker.serialization.readMatches

internal class StaticMockProvider(
    private val filingPolicy: FilingPolicy,
    private val loadFileContent: (String) -> StreamReader?,
    private val mapper: Mapper
) : ScenarioProvider {

    private val logger = getLogger()

    private val matcher = RequestMatcher()

    override fun loadResponse(request: HttpRequest): ResponseDescriptor? = when (val result = loadResult(request)) {
        is ResponseDescriptor -> result
        is NetworkError -> throwError(result)
        else -> null
    }

    private fun loadResult(request: HttpRequest): RequestResult? = try {
        val path = filingPolicy.getPath(request)
        logger.info("Loading scenarios from $path")
        loadAndMatchResponse(path, request)
    } catch (e: IOException) {
        logger.error("Scenario file could not be loaded. Returning null.")
        null
    } catch (e: Exception) {
        logger.error("Scenario file could not be loaded", e)
        val stackTrace = e.stackTraceToString()
        ResponseDescriptor(code = 404, body = "${e::class.simpleName}: ${e.message}\n\tat $stackTrace")
    }

    private fun loadAndMatchResponse(path: String, request: HttpRequest) =
        loadFileContent(path)?.let { stream ->
            val list = mapper.readMatches(stream)
            matchRequest(request, list)
        }

    private fun matchRequest(request: HttpRequest, list: List<Matcher>?): RequestResult? =
        list?.firstOrNull { matcher.matchRequest(it.request, request) }?.result
            ?.buildResponseBody(request)
            .also { logger.info(if (it != null) "Match found" else "No match for request") }

    private fun RequestResult.buildResponseBody(request: HttpRequest): RequestResult = if (this is ResponseDescriptor) {
        val body = bodyFile?.let { loadResponseFromFile(request, it) } ?: body
        copy(body = body, bodyFile = null)
    } else this

    private fun loadResponseFromFile(request: HttpRequest, path: String): String? {
        logger.info("Loading response body from file: $path")
        return loadFileContent(getRelativePath(filingPolicy.getPath(request), path))?.readAsString()
    }

    private fun getRelativePath(base: String, child: String): String =
        concatenatePaths(base, child).cleanFolderList().joinToString("/")

    private fun concatenatePaths(base: String, child: String) =
        base.split("/").dropLast(1) + child.split("/")

    private fun List<String>.cleanFolderList() = filterIndexed { index, segment ->
        segment != ".." && (index == size - 1 || get(index + 1) != "..")
    }

    private fun throwError(error: NetworkError): Nothing {
        throw ClassloaderUtils().createException(error)
    }

    override fun toString(): String = "static mock configuration"
}
