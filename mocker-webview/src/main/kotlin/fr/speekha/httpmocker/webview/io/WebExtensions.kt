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

package fr.speekha.httpmocker.webview.io

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.messageForHttpCode
import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.model.ResponseDescriptor
import java.io.ByteArrayInputStream

private const val DEFAULT_HTTP_PORT = 80
private const val DEFAULT_HTTPS_PORT = 443
private const val NO_PORT = -1

internal fun WebResourceRequest.toGenericModel() = HttpRequest(
    method = method,
    scheme = url.scheme ?: "https",
    host = url.host ?: "",
    port = url.resolvedPort,
    path = url.encodedPath ?: "/",
    params = url.parseQueryParameters(),
    headers = requestHeaders.entries.map { NamedParameter(it.key, it.value) }
)

internal fun ResponseDescriptor.toWebResourceResponse() = WebResourceResponse(
    mediaType,
    "UTF-8",
    code,
    messageForHttpCode(code),
    headers.associate { it.name to (it.value ?: "") },
    ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
)

private fun Uri.parseQueryParameters(): List<NamedParameter> =
    queryParameterNames.flatMap { name ->
        getQueryParameters(name).map { NamedParameter(name, it) }
    }

private val Uri.resolvedPort: Int
    get() = if (port != NO_PORT) port else defaultPortFor(scheme)

private fun defaultPortFor(scheme: String?): Int = when (scheme) {
    "https" -> DEFAULT_HTTPS_PORT
    else -> DEFAULT_HTTP_PORT
}
