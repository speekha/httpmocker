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

package fr.speekha.httpmocker.kotlinx

import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.list
import kotlinx.serialization.modules.EmptyModule
import fr.speekha.httpmocker.kotlinx.Matcher as JsonMatcher

@UnstableDefault
/**
 * A mapper using Kotlinx serialization to serialize/deserialize scenarios.
 * Common JSON format available with other mappers is not compatible with Kotlinx Serialization,
 * so this mapper accepts transformation functions to handle the conversion between the common
 * format and the one supported by Kotlinx Serialization.
 * @see fr.speekha.httpmocker.kotlinx.JsonFormatConverter
 * @param formatInput transformation function to apply when reading JSON
 * @param formatOutput transformation function to apply when writing JSON
 */
class KotlinxMapper(
    private val formatInput: (String) -> String = { it },
    private val formatOutput: (String) -> String = { it }
) : Mapper {

    private val adapter = Json {
        encodeDefaults = false
        prettyPrint = true
        strictMode = true
        unquoted = false
        indent = " "
        useArrayPolymorphism = false
        classDiscriminator = "type"
        serialModule = EmptyModule
    }

    override fun deserialize(payload: String): List<Matcher> =
        parseMatcherList(adapter.parseJson(formatInput(payload)))

    private fun parseMatcherList(json: JsonElement): List<Matcher> = json.jsonArray.map {
        it.toMatcher()
    }

    override fun serialize(matchers: List<Matcher>): String = formatOutput(
        adapter.stringify(JsonMatcher.serializer().list, matchers.map { JsonMatcher(it) })
    )
}

private fun JsonElement.toMatcher(): Matcher =
    Matcher(jsonObject["request"].toRequest(), jsonObject["response"].toResponse())

private fun JsonElement?.toRequest(): RequestDescriptor = this?.run {
    RequestDescriptor(
        jsonObject["exact-match"]?.primitive?.boolean ?: false,
        jsonObject["protocol"]?.asLiteral(),
        jsonObject["method"]?.asLiteral(),
        jsonObject["host"]?.asLiteral(),
        jsonObject["port"]?.primitive?.int,
        jsonObject["path"]?.asLiteral(),
        jsonObject["headers"].toHeaders(),
        jsonObject["params"].toParams(),
        jsonObject["body"]?.asLiteral()
    )
} ?: RequestDescriptor()

private fun JsonElement?.toResponse(): ResponseDescriptor = this?.run {
    var result = ResponseDescriptor()
    jsonObject["delay"]?.let { result = result.copy(delay = it.primitive.long) }
    jsonObject["code"]?.let { result = result.copy(code = it.primitive.int) }
    jsonObject["media-type"]?.let { result = result.copy(mediaType = it.asLiteral()) }
    jsonObject["headers"]?.let { result = result.copy(headers = jsonObject["headers"].toHeaders()) }
    jsonObject["body"]?.let { result = result.copy(body = it.asLiteral()) }
    jsonObject["body-file"]?.let { result = result.copy(bodyFile = it.asLiteral()) }
    result
} ?: ResponseDescriptor()

private fun JsonElement?.toParams(): Map<String, String> = this?.run {
    jsonObject.mapValues { it.value.asLiteral() }
} ?: mapOf()

private fun JsonElement?.toHeaders(): List<Header> = this?.run {
    jsonArray.map { Header(it.jsonObject["name"].asLiteral(), it.jsonObject["value"].asLiteral()) }
} ?: listOf()

private fun JsonElement?.asLiteral(): String = (this as? JsonLiteral)?.body?.toString() ?: ""
