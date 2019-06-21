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

import fr.speekha.httpmocker.model.ResponseDescriptor

class ResponseAdapter : ObjectAdapter<ResponseDescriptor>{

    override fun fromJson(reader: JsonStringReader): ResponseDescriptor {
        var response = ResponseDescriptor()
        reader.beginObject()
        while (reader.hasNext()) {
            response = when (val field = reader.readFieldName()) {
                "delay" -> response.copy(delay= reader.readLong())
                "code" -> response.copy(code= reader.readInt())
                "media-type" -> response.copy(mediaType = reader.readString())
                "headers" -> response.copy(headers = reader.readObject(HeaderListAdapter()))
                "body" -> response.copy(body = reader.readString())
                "body-file" -> response.copy(bodyFile = reader.readString())
                else -> error("Unknown field $field")
            }
            reader.next()
        }
        reader.endObject()
        return response
    }

}