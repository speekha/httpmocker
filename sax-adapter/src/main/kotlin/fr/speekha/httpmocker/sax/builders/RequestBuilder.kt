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

package fr.speekha.httpmocker.sax.builders

import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.RequestTemplate
import org.xml.sax.Attributes

class RequestBuilder(
    private val parent: CaseBuilder,
    attributes: Attributes?
) : NodeBuilder(),
    NodeWithHeaders,
    NodeWithBody {

    override var headers = mutableListOf<Header>()

    override var body: String? = null

    override var bodyFile: String?
        get() = error("Body file not supported in requests")
        set(_) = error("Body file not supported in requests")

    private var params = mutableMapOf<String, String?>()

    private val exactMatch = attributes?.getValue("exact-match")?.toBoolean() ?: false

    private var protocol: String? = null

    private var method: String? = null

    private var host: String? = null

    private var port: Int? = null

    private var path: String? = null

    override fun build() {
        parent.setRequest(
            RequestTemplate(
                exactMatch = exactMatch,
                protocol = protocol,
                method = method,
                host = host,
                port = port,
                path = path,
                headers = headers,
                params = params,
                body = body
            )
        )
    }

    fun setUrlAttributes(
        protocol: String?,
        method: String?,
        host: String?,
        port: Int?,
        path: String?
    ) {
        this.protocol = protocol
        this.method = method
        this.host = host
        this.port = port
        this.path = path
    }

    fun addQueryParam(key: String, value: String?) {
        params[key] = value
    }
}
