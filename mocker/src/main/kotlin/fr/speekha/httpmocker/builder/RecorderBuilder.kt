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

package fr.speekha.httpmocker.builder

import fr.speekha.httpmocker.policies.FilingPolicy
import okhttp3.Request
import java.io.File

class RecorderBuilder(
    internal var rootFolder: File?,
    internal var policy: FilingPolicy? = null
) {

    /**
     * Defines the policy used to name the scenario files based on the request being intercepted
     * @param filingPolicy the naming policy to use for scenario files
     */
    infix fun with(filingPolicy: FilingPolicy) = apply {
        policy = filingPolicy
    }

    /**
     * Defines the policy used to name the scenario files based on the request being intercepted
     * @param filingPolicy a lambda to use as the naming policy for scenario files
     */
    infix fun with(filingPolicy: (Request) -> String) = apply {
        policy = FilingPolicyBuilder(filingPolicy)
    }
}
