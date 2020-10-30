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

import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestTemplate
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.serialization.BODY
import fr.speekha.httpmocker.serialization.CODE
import fr.speekha.httpmocker.serialization.DELAY
import fr.speekha.httpmocker.serialization.ERROR
import fr.speekha.httpmocker.serialization.EXACT_MATCH
import fr.speekha.httpmocker.serialization.EXCEPTION_TYPE
import fr.speekha.httpmocker.serialization.HEADER
import fr.speekha.httpmocker.serialization.HOST
import fr.speekha.httpmocker.serialization.MEDIA_TYPE
import fr.speekha.httpmocker.serialization.METHOD
import fr.speekha.httpmocker.serialization.PARAM
import fr.speekha.httpmocker.serialization.PATH
import fr.speekha.httpmocker.serialization.PORT
import fr.speekha.httpmocker.serialization.PROTOCOL
import fr.speekha.httpmocker.serialization.REQUEST
import fr.speekha.httpmocker.serialization.RESPONSE
import fr.speekha.httpmocker.serialization.URL

internal fun List<Matcher>.toXml() = XML_PREFACE + writeTags("scenarios", 0) {
    toXml { it.toXml(1) }
}

private fun Matcher.toXml(indent: Int): String = writeTags("case", indent) {
    val subIndent = indent + 1
    writeTagList(
        request.toXml(subIndent),
        response?.toXml(subIndent).orEmpty(),
        error?.toXml(subIndent).orEmpty()
    )
}

private fun RequestTemplate.toXml(indentation: Int): String =
    writeTags(REQUEST, indentation, exactMatchAttribute()) {
        val subIndentation = indentation + 1
        writeTagList(
            writeUrl(getUrlAttributes(), params, subIndentation),
            writeHeaders(headers, subIndentation),
            if (body.isNullOrEmpty()) "" else writeCData(BODY, subIndentation, body = body)
        )
    }

private fun RequestTemplate.getUrlAttributes(): List<Pair<String, Any?>> = listOf(
    PROTOCOL to protocol,
    METHOD to method,
    HOST to host,
    PORT to port,
    PATH to path
)

private fun RequestTemplate.exactMatchAttribute() =
    listOf(EXACT_MATCH to true).takeIf { exactMatch } ?: emptyList()

private fun ResponseDescriptor.toXml(indentation: Int): String = writeTags(
    RESPONSE,
    indentation,
    listOf(DELAY to delay, CODE to code, MEDIA_TYPE to mediaType)
) {
    val subIndentation = indentation + 1
    writeTagList(
        writeHeaders(headers, subIndentation),
        writeCData("body", subIndentation, listOf("file" to bodyFile), body)
    )
}

private fun NetworkError.toXml(indent: Int): String = writeCData(
    tag = ERROR,
    indentation = indent,
    attributes = listOf(EXCEPTION_TYPE to exceptionType),
    body = message
)

private fun writeUrl(
    attributes: List<Pair<String, Any?>>,
    params: Map<String, String?>,
    indent: Int
): String = if (attributes.any { it.second != null } || params.isNotEmpty()) {
    writeTags(URL, indent, attributes) { params.toXml(indent + 1) }
} else {
    ""
}

private fun writeHeaders(headers: List<Header>, indent: Int): String = if (headers.isEmpty()) {
    ""
} else {
    headers.toXml { it.toXml(indent) }
}

private fun Header.toXml(indent: Int): String =
    writeCData(HEADER, indent, listOf("name" to name), value)

private fun Map<String, String?>.toXml(indent: Int): String = entries.toXml {
    writeCData(PARAM, indent, listOf("name" to it.key), it.value)
}

private fun writeTags(
    tag: String,
    indentation: Int,
    attributes: List<Pair<String, Any?>> = emptyList(),
    body: () -> String?
): String {
    val content = body()
    val processedContent =
        if (content.isNullOrEmpty()) null else "\n$content\n${indent(indentation)}"
    return writeTag(tag, indentation, attributes, processedContent)
}

private fun writeTag(
    tag: String,
    indentation: Int,
    attributes: List<Pair<String, Any?>> = emptyList(),
    body: String? = null
): String = if (body != null || attributes.filter { it.second != null }.isNotEmpty()) {
    StringBuilder(indent(indentation))
        .append("<")
        .append(tag)
        .append(
            attributes.filter { it.second != null }
                .joinToString("") { (name, value) -> " $name=\"$value\"" }
        )
        .append(body?.let { ">$it</$tag>" } ?: " />").toString()
} else {
    ""
}

private fun <T : Any?> Iterable<T>.toXml(format: (T) -> String? = { it?.toString() }): String =
    mapNotNull(format)
        .filter { it.isNotEmpty() }
        .joinToString("\n")

private fun <T : Any?> writeTagList(
    vararg tags: T,
    format: (T) -> String? = { it?.toString() }
): String = tags.map { it }.toXml(format)

private fun writeCData(
    tag: String,
    indentation: Int,
    attributes: List<Pair<String, Any?>> = emptyList(),
    body: String? = null
): String {
    val formatBody = if (body?.contains("[<>]".toRegex()) == true) "<![CDATA[$body]]>" else body
    return StringBuilder(indent(indentation))
        .append("<")
        .append(tag)
        .append(
            attributes.filter { it.second != null }
                .joinToString("") { (name, value) -> " $name=\"$value\"" }
        )
        .append(body?.let { ">$formatBody</$tag>" } ?: " />").toString()
}

private fun indent(spaces: Int) = " ".repeat(spaces * SPACE_PER_TAB)

private operator fun StringBuilder.plusAssign(obj: Any) {
    append(obj)
}

private fun String?.orEmpty() = this ?: ""

private const val XML_PREFACE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"

private const val SPACE_PER_TAB: Int = 4
