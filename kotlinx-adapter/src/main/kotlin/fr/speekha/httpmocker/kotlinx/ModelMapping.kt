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

import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestTemplate
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.serialization.*
import kotlinx.serialization.json.*
import fr.speekha.httpmocker.model.NamedParameter as ModelHeader

internal fun JsonElement.toMatcher(): Matcher = Matcher(
    jsonObject[REQUEST]?.toRequest() ?: RequestTemplate(),
    jsonObject[RESPONSE]?.toResponse(),
    jsonObject[ERROR]?.toError()
)

private fun JsonElement.toRequest(): RequestTemplate = RequestTemplate(
    jsonObject[EXACT_MATCH]?.jsonPrimitive?.boolean ?: false,
    jsonObject[PROTOCOL]?.asNullableLiteral(),
    jsonObject[METHOD]?.asNullableLiteral(),
    jsonObject[HOST]?.asNullableLiteral(),
    jsonObject[PORT]?.jsonPrimitive?.int,
    jsonObject[PATH]?.asNullableLiteral(),
    jsonObject[HEADERS].toHeaders(),
    jsonObject[PARAMS].toParams(),
    jsonObject[BODY]?.asNullableLiteral()
)

private fun JsonElement.toResponse(): ResponseDescriptor = ResponseDescriptor()
    .update(this, DELAY) { copy(delay = it.jsonPrimitive.long) }
    .update(this, CODE) { copy(code = it.jsonPrimitive.int) }
    .update(this, MEDIA_TYPE) { copy(mediaType = it.asLiteral()) }
    .update(this, HEADERS) { copy(headers = it.toHeaders()) }
    .update(this, BODY) { copy(body = it.asLiteral()) }
    .update(this, BODY_FILE) { copy(bodyFile = it.asNullableLiteral()) }

private fun ResponseDescriptor.update(
    jsonElement: JsonElement?,
    field: String,
    updateObject: ResponseDescriptor.(JsonElement) -> ResponseDescriptor
): ResponseDescriptor =
    jsonElement?.jsonObject?.get(field)?.let { updateObject(it) } ?: this

private fun JsonElement.toError(): NetworkError = NetworkError(
    jsonObject[EXCEPTION_TYPE]?.asNullableLiteral() ?: "",
    jsonObject[EXCEPTION_MESSAGE]?.asNullableLiteral()
)

private fun JsonElement?.toParams(): List<ModelHeader> = this?.jsonArray?.map {
    ModelHeader(
        it.jsonObject[NAME].asNullableLiteral() ?: error("Incorrect header name"),
        it.jsonObject[VALUE].asNullableLiteral()
    )
} ?: listOf()

private fun JsonElement?.toHeaders(): List<ModelHeader> = this?.jsonArray?.map {
    fr.speekha.httpmocker.model.NamedParameter(
        it.jsonObject[NAME].asNullableLiteral() ?: error("Incorrect header name"),
        it.jsonObject[VALUE].asNullableLiteral()
    )
} ?: listOf()

private fun JsonElement?.asLiteral(): String = (this as? JsonPrimitive)?.contentOrNull ?: ""

private fun JsonElement?.asNullableLiteral(): String? = (this as? JsonPrimitive)?.contentOrNull
