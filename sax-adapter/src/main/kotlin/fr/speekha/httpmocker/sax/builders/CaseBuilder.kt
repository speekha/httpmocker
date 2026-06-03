/*
 * Copyright 2019-2021 David Blanc
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

package fr.speekha.httpmocker.sax.builders

import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.RequestTemplate
import fr.speekha.httpmocker.model.ResponseDescriptor

class CaseBuilder(val parent: ScenariosBuilder) : NodeBuilder() {

    private var request: RequestTemplate? = null
    private var response: ResponseDescriptor? = null
    private var error: NetworkError? = null

    override fun build() = parent.add(
        Matcher(
            request = request ?: RequestTemplate(),
            response = response,
            error = error
        )
    )

    fun setResponse(value: ResponseDescriptor) {
        response = value
    }

    fun setRequest(value: RequestTemplate) {
        request = value
    }

    fun setError(value: NetworkError) {
        error = value
    }
}
