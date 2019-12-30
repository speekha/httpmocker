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

/**
 * Adapter to convert a JSON snippet to an object of type T. That adapter must implement all
 * the necessary steps to decode the JSON string into the corresponding object.
 * @param T type of the object to return when parsing JSON
 */
interface ObjectAdapter<T : Any> {

    /**
     * Parses the JSON block to instantiate an object
     * @param reader the JSON reader to use to access the JSON data
     * @return the concrete object for that JSON
     */
    fun fromJson(reader: JsonStringReader): T
}
