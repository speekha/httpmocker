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

package fr.speekha.httpmocker.builder

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.io.RequestWriter
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.scenario.ScenarioProvider
import fr.speekha.httpmocker.serialization.Mapper

/**
 * Builder to instantiate an interceptor.
 */
data class Config internal constructor(
    val filingPolicy: List<FilingPolicy>,
    val openFile: FileLoader?,
    val mapper: Mapper?,
    val simulatedDelay: Long,
    var status: Mode,
    val showSavingErrors: Boolean,
    val providers: List<ScenarioProvider>,
    val requestWriter: RequestWriter?
) {
    constructor() : this(
        filingPolicy = emptyList(),
        openFile = null,
        mapper = null,
        simulatedDelay = 0,
        status = Mode.DISABLED,
        showSavingErrors = false,
        providers = emptyList(),
        requestWriter = null
    )

    operator fun plus(policy: FilingPolicy) = copy(filingPolicy = filingPolicy + policy)
}
