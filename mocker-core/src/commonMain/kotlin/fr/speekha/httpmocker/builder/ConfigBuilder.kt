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
import fr.speekha.httpmocker.io.FileAccessor
import fr.speekha.httpmocker.io.RequestWriter
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.MirrorPathPolicy
import fr.speekha.httpmocker.scenario.DynamicMockProvider
import fr.speekha.httpmocker.scenario.RequestCallback
import fr.speekha.httpmocker.scenario.ScenarioProvider
import fr.speekha.httpmocker.scenario.StaticMockProvider
import fr.speekha.httpmocker.serialization.Mapper

/**
 * Builder to instantiate an interceptor.
 */
class ConfigBuilder internal constructor(
    private var config: Config,
    private val dynamicCallbacks: MutableList<RequestCallback>
) : Configurator {

    constructor() : this(
        config = Config(),
        dynamicCallbacks = mutableListOf()
    )

    var recorder: RecorderBuilder? = null

    /**
     * {@inheritdoc}.
     */
    override fun decodeScenarioPathWith(policy: FilingPolicy) {
        config += policy
    }

    /**
     * {@inheritdoc}.
     */
    override fun loadFileWith(loading: FileLoader) {
        config = config.copy(openFile = wrapLoadingExceptions(loading))
    }

    /**
     * {@inheritdoc}.
     */
    override fun useDynamicMocks(callback: RequestCallback) {
        dynamicCallbacks += callback
    }

    /**
     * {@inheritdoc}.
     */
    override fun parseScenariosWith(objectMapper: Mapper) {
        config = config.copy(mapper = objectMapper)
    }

    /**
     * {@inheritdoc}.
     */
    override fun recordScenariosIn(folder: FileAccessor, policy: FilingPolicy?) {
        recorder = RecorderBuilder(folder, policy)
    }

    /**
     * {@inheritdoc}.
     */
    override fun failOnRecordingError(failOnError: Boolean) {
        config = config.copy(showSavingErrors = failOnError)
    }

    /**
     * {@inheritdoc}.
     */
    override fun addFakeNetworkDelay(delay: Long) {
        config = config.copy(simulatedDelay = delay)
    }

    /**
     * {@inheritdoc}.
     */
    override fun setMode(mode: Mode) {
        config.mode = mode
    }

    /**
     * {@inheritdoc}.
     */
    override fun buildConfig(): Config =
        config.copy(providers = buildProviders(), requestWriter = buildRecorder())

    private fun buildProviders(): List<ScenarioProvider> {
        val dynamicMockProvider =
            dynamicCallbacks.takeIf { it.isNotEmpty() }?.let { DynamicMockProvider(it) }
        val staticMockProvider = buildStaticProvider()
        return listOfNotNull(dynamicMockProvider) + staticMockProvider
    }

    private fun buildStaticProvider(): List<StaticMockProvider> = with(config) {
        if (mapper != null && openFile != null) {
            var policies = filingPolicy
            if (policies.isEmpty()) {
                policies = listOf(MirrorPathPolicy(mapper.supportedFormat))
            }
            policies.map {
                StaticMockProvider(it, openFile::load, mapper)
            }
        } else {
            emptyList()
        }
    }

    private fun buildRecorder() = with(config) {
        recorder?.let {
            if (mapper != null) {
                RequestWriter(
                    mapper,
                    recorder?.policy
                        ?: filingPolicy.getOrNull(0)
                        ?: MirrorPathPolicy(mapper.supportedFormat),
                    it.rootFolder,
                    showSavingErrors
                )
            } else {
                null
            }
        }
    }
}
