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

import fr.speekha.httpmocker.model.NamedParameter
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.serialization.DEFAULT_MEDIA_TYPE
import fr.speekha.httpmocker.serialization.DEFAULT_RESPONSE_CODE
import org.xml.sax.Attributes

class ResponseBuilder(
    private val parent: CaseBuilder,
    attributes: Attributes?
) : NodeBuilder(),
    NodeWithHeaders,
    NodeWithBody {

    private val delay: Long = attributes?.getValue("delay")?.toLong() ?: 0
    private val code: Int = attributes?.getValue("code")?.toInt() ?: DEFAULT_RESPONSE_CODE
    private val mediatype: String = attributes?.getValue("media-type") ?: DEFAULT_MEDIA_TYPE
    override var body: String? = null
    override var bodyFile: String? = null
    override var headers = mutableListOf<NamedParameter>()

    override fun build() = parent.setResponse(
        ResponseDescriptor(
            delay = delay,
            code = code,
            mediaType = mediatype,
            headers = headers,
            body = body ?: "",
            bodyFile = bodyFile
        )
    )
}
