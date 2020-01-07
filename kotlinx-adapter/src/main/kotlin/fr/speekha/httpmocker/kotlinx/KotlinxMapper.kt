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
import fr.speekha.httpmocker.serialization.JSON_FORMAT
import fr.speekha.httpmocker.serialization.Mapper
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.list
import kotlinx.serialization.modules.EmptyModule
import fr.speekha.httpmocker.kotlinx.model.Matcher as JsonMatcher

@UnstableDefault
/**
 * A mapper using Kotlinx serialization to serialize/deserialize scenarios.
 * The common JSON format available with other mappers is not compatible with Kotlinx Serialization,
 * so this mapper accepts transformation functions to handle the conversion between the common
 * compact format and the one supported by Kotlinx Serialization.
 * @see fr.speekha.httpmocker.serialization.JsonFormatConverter
 * @param formatInput transformation function to apply when reading JSON
 * @param formatOutput transformation function to apply when writing JSON
 */
class KotlinxMapper(
    private val formatInput: (String) -> String = { it },
    private val formatOutput: (String) -> String = { it }
) : Mapper {

    override val supportedFormat: String = JSON_FORMAT

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
