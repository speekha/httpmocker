/*
 * Copyright 2019-2020 David Blanc
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

package fr.speekha.httpmocker.scenario

import fr.speekha.httpmocker.io.HttpRequest
import fr.speekha.httpmocker.model.ResponseDescriptor

/**
 * A callback to provide a dynamic mock response.
 */
fun interface RequestCallback {

    /**
     * Computes the appropriate response for a request
     * @param request the request to answer
     * @return the description of the mock response
     */
    fun processRequest(request: HttpRequest): ResponseDescriptor?
}
