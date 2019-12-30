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

package fr.speekha.httpmocker.custom.adapters

import fr.speekha.httpmocker.custom.JsonStringReader
import fr.speekha.httpmocker.custom.unknownFieldError
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.serialization.ERROR
import fr.speekha.httpmocker.serialization.REQUEST
import fr.speekha.httpmocker.serialization.RESPONSE

internal class MatcherAdapter : BaseObjectAdapter<Matcher>() {

    override fun createObject(): Matcher = Matcher()

    override fun updateObject(
        reader: JsonStringReader,
        builder: Matcher
    ): Matcher = when (val field = reader.readFieldName()) {
        REQUEST -> builder.copy(request = reader.readObject(RequestAdapter()))
        RESPONSE -> builder.copy(response = reader.readObject(ResponseAdapter()))
        ERROR -> builder.copy(error = reader.readObject(ErrorAdapter()))
        else -> unknownFieldError(field)
    }
}
