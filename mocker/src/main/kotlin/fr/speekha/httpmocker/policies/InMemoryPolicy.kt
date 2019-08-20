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

package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.writeValue
import okhttp3.Request
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * A filing policy allowing to configure requests programmatically. It can be used with a
 * @see fr.speekha.httpmocker.MockResponseInterceptor like this:
 * <pre>{@code
 * val policy = InMemoryPolicy()
 * val interceptor = MockResponseInterceptor(policy, policy::matchRequest)
 * }</pre>
 */
@Deprecated("Dynamic mocks are a better way to mock calls programmatically")
class InMemoryPolicy(
    private val mapper: Mapper
) : FilingPolicy {

    private val matchers = mutableMapOf<String, List<Matcher>>()

    override fun getPath(request: Request): String = request.url().toString()

    /**
     * Adds a new rule for a certain URL
     * @param url the URL being matched by this rule
     * @param matcher the detailed request and response included in this rule
     */
    fun addMatcher(url: String, matcher: Matcher) {
        matchers[url] = (matchers[url] ?: listOf()) + matcher
    }

    /**
     * The loading function needed by the interceptor
     */
    fun matchRequest(url: String): InputStream? = matchers[url]?.let {
        PipedInputStream().apply {
            val pipeOut = PipedOutputStream()
            pipeOut.connect(this)
            mapper.writeValue(pipeOut, it)
        }
    }
}
