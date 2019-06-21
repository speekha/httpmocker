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

package fr.speekha.httpmocker.custom

import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.ResponseDescriptor

class MatcherAdapter : ObjectAdapter<Matcher> {

    override fun fromJson(reader: JsonStringReader): Matcher {
        var matcher = Matcher(response = ResponseDescriptor())
        reader.beginObject()
        while (reader.hasNext()) {
            matcher = when (val field = reader.readFieldName()) {
                "request" -> matcher.copy(request = reader.readObject(RequestAdapter()))
                "response" -> matcher.copy(response = reader.readObject(ResponseAdapter()))
                else -> error("Unknown field $field")
            }
            reader.next()
        }
        reader.endObject()
        return matcher
    }

}