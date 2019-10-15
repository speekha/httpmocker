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

package fr.speekha.httpmocker.sax

import org.xml.sax.Attributes


class UrlBuilder(
    private val parent: RequestBuilder,
    attributes: Attributes?
) : Builder {

    private val protocol = attributes?.getValue("protocol")
    private val method = attributes?.getValue("method")
    private val host = attributes?.getValue("host")
    private val port = attributes?.getValue("port")
    private val path = attributes?.getValue("path")

    override fun build() {
        parent.setUrlAttributes(protocol, method, host, port?.toInt(), path)
    }

    fun addQueryParam(key: String, value: String?) {
        parent.addQueryParam(key, value)
    }

}
