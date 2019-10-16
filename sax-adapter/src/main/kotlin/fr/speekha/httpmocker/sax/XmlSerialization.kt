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

import fr.speekha.httpmocker.BODY
import fr.speekha.httpmocker.EXACT_MATCH
import fr.speekha.httpmocker.HEADERS
import fr.speekha.httpmocker.HOST
import fr.speekha.httpmocker.METHOD
import fr.speekha.httpmocker.PATH
import fr.speekha.httpmocker.PORT
import fr.speekha.httpmocker.PROTOCOL
import fr.speekha.httpmocker.REQUEST
import fr.speekha.httpmocker.RESPONSE
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor

internal fun List<Matcher>.toXml() =
    XML_PREFACE + writeTags("scenarios", 0) {
        writeTagList { it.toXml(1) }
    }

private fun Matcher.toXml(indent: Int): String = writeTags("case", indent) {
    request.toXml(indent) + response?.toXml(indent).orEmpty() + error?.toXml(indent).orEmpty()
}

private fun RequestDescriptor.toXml(indent: Int): String = writeTags(
    REQUEST,
    indent,
    listOf(EXACT_MATCH to true).takeIf { exactMatch } ?: emptyList()) {
    writeUrl(
        listOf(
            PROTOCOL to protocol,
            METHOD to method,
            HOST to host,
            PORT to port,
            PATH to path
        ), params, indent + 1
    ) + writeHeaders(headers, indent) + if (body.isNullOrEmpty()) {
        ""
    } else {
        writeTag(BODY, indent + 1, body = body)
    }
}

private fun ResponseDescriptor.toXml(indent: Int): String = writeTags(
    RESPONSE,
    indent,
    listOf("delay" to delay, "code" to code, "media-type" to mediaType)
) {
    writeHeaders(headers, indent + 1) + writeTag(
        "body",
        indent + 1,
        listOf("file" to bodyFile),
        body
    )
}

private fun NetworkError.toXml(indent: Int): String =
    writeTag("error", indent, listOf("type" to exceptionType), message)

private fun writeUrl(
    attributes: List<Pair<String, Any?>>,
    params: Map<String, String?>,
    indent: Int
): String = if (attributes.any { it.second != null } || params.isNotEmpty()) {
    writeTags(
        "url",
        indent,
        attributes
    ) { params.toXml(indent + 1) }
} else {
    ""
}

private fun writeHeaders(headers: List<Header>, indent: Int): String = if (headers.isEmpty()) {
    ""
} else {
    writeTags(HEADERS, indent + 1) {
        headers.writeTagList { it.toXml(indent + 2) }
    }
}

private fun Header.toXml(indent: Int): String =
    writeTag("header", indent, listOf("name" to name), value)

private fun Map<String, String?>.toXml(indent: Int): String = entries.writeTagList {
    writeTag("param", indent, listOf("name" to it.key), it.value)
}

private fun writeTags(
    tag: String,
    indent: Int,
    attributes: List<Pair<String, Any?>> = emptyList(),
    body: () -> String?
): String {
    val content = body()
    val processedContent = if (content.isNullOrEmpty()) null else "\n$content\n"
    return writeTag(tag, indent, attributes, processedContent)
}

private fun writeTag(
    tag: String,
    indent: Int,
    attributes: List<Pair<String, Any?>> = emptyList(),
    body: String? = null
): String = StringBuilder(" ".repeat(indent * SPACE_PER_TAB))
    .append("<")
    .append(tag)
    .append(
        attributes.filter { it.second != null }
            .joinToString("") { (name, value) -> " $name=\"$value\"" }
    )
    .append(body?.let { ">$it</$tag>\n" } ?: " />\n").toString()

private fun <T : Any> Iterable<T>.writeTagList(format: (T) -> String): String = joinToString("") {
    format(it)
}

private operator fun StringBuilder.plusAssign(obj: Any) {
    append(obj)
}

private fun String?.orEmpty() = this ?: ""

private const val XML_PREFACE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
private const val SPACE_PER_TAB: Int = 4
