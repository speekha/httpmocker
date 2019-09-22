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

import fr.speekha.httpmocker.BODY
import fr.speekha.httpmocker.BODY_FILE
import fr.speekha.httpmocker.CODE
import fr.speekha.httpmocker.DELAY
import fr.speekha.httpmocker.ERROR
import fr.speekha.httpmocker.EXACT_MATCH
import fr.speekha.httpmocker.EXCEPTION_MESSAGE
import fr.speekha.httpmocker.EXCEPTION_TYPE
import fr.speekha.httpmocker.HEADERS
import fr.speekha.httpmocker.HOST
import fr.speekha.httpmocker.MEDIA_TYPE
import fr.speekha.httpmocker.METHOD
import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.NAME
import fr.speekha.httpmocker.PARAMS
import fr.speekha.httpmocker.PATH
import fr.speekha.httpmocker.PORT
import fr.speekha.httpmocker.PROTOCOL
import fr.speekha.httpmocker.REQUEST
import fr.speekha.httpmocker.RESPONSE
import fr.speekha.httpmocker.VALUE
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.list
import kotlinx.serialization.modules.EmptyModule
import fr.speekha.httpmocker.kotlinx.Matcher as JsonMatcher
import fr.speekha.httpmocker.model.Header as ModelHeader

@UnstableDefault
/**
 * A mapper using Kotlinx serialization to serialize/deserialize scenarios.
 * The common JSON format available with other mappers is not compatible with Kotlinx Serialization,
 * so this mapper accepts transformation functions to handle the conversion between the common
 * compact format and the one supported by Kotlinx Serialization.
 * @see fr.speekha.httpmocker.JsonFormatConverter
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

private fun JsonElement.toMatcher(): Matcher = Matcher(
    jsonObject[REQUEST]?.toRequest() ?: RequestDescriptor(),
    jsonObject[RESPONSE].toResponse(),
    jsonObject[ERROR]?.toError()
)

private fun JsonElement.toRequest(): RequestDescriptor = RequestDescriptor(
    jsonObject[EXACT_MATCH]?.primitive?.boolean ?: false,
    jsonObject[PROTOCOL]?.asNullableLiteral(),
    jsonObject[METHOD]?.asNullableLiteral(),
    jsonObject[HOST]?.asNullableLiteral(),
    jsonObject[PORT]?.primitive?.int,
    jsonObject[PATH]?.asNullableLiteral(),
    jsonObject[HEADERS].toHeaders(),
    jsonObject[PARAMS].toParams(),
    jsonObject[BODY]?.asNullableLiteral()
)

private fun JsonElement?.toResponse(): ResponseDescriptor = ResponseDescriptor()
    .update(this, DELAY) { copy(delay = it.primitive.long) }
    .update(this, CODE) { copy(code = it.primitive.int) }
    .update(this, MEDIA_TYPE) { copy(mediaType = it.asLiteral()) }
    .update(this, HEADERS) { copy(headers = it.toHeaders()) }
    .update(this, BODY) { copy(body = it.asLiteral()) }
    .update(this, BODY_FILE) { copy(bodyFile = it.asNullableLiteral()) }

private fun JsonElement.toError(): NetworkError = NetworkError(
    jsonObject[EXCEPTION_TYPE]?.asNullableLiteral() ?: "",
    jsonObject[EXCEPTION_MESSAGE]?.asNullableLiteral() ?: ""
)

private fun ResponseDescriptor.update(
    jsonElement: JsonElement?,
    field: String,
    updateObject: ResponseDescriptor.(JsonElement) -> ResponseDescriptor
): ResponseDescriptor =
    jsonElement?.jsonObject?.get(field)?.let { updateObject(it) } ?: this

private fun JsonElement?.toParams(): Map<String, String?> =
    this?.jsonObject?.mapValues { it.value.asNullableLiteral() } ?: mapOf()

private fun JsonElement?.toHeaders(): List<ModelHeader> = this?.jsonArray?.map {
    ModelHeader(
        it.jsonObject[NAME].asNullableLiteral() ?: error("Incorrect header name"),
        it.jsonObject[VALUE].asNullableLiteral()
    )
} ?: listOf()

private fun JsonElement?.asLiteral(): String = (this as? JsonLiteral)?.body?.toString() ?: ""
private fun JsonElement?.asNullableLiteral(): String? = (this as? JsonLiteral)?.body?.toString()
