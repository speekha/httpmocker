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

internal class DynamicMockProvider(
    private val callbacks: List<RequestCallback>
) : ScenarioProvider {

    override fun loadResponse(request: HttpRequest): ResponseDescriptor? = callbacks
        .asSequence()
        .mapNotNull { it.processRequest(request)?.copy(bodyFile = null) }
        .firstOrNull()

    override fun toString(): String = "dynamic mock configuration"
}
