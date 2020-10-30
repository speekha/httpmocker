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

package fr.speekha.httpmocker.builder

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.MirrorPathPolicy
import fr.speekha.httpmocker.scenario.DynamicMockProvider
import fr.speekha.httpmocker.scenario.RequestCallback
import fr.speekha.httpmocker.scenario.ScenarioProvider
import fr.speekha.httpmocker.scenario.StaticMockProvider
import fr.speekha.httpmocker.serialization.Mapper
import java.io.File

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
     * For static mocks: Defines the policy used to retrieve the configuration files based
     * on the request being intercepted
     * @param policy the naming policy to use for scenario files
     */
    override fun decodeScenarioPathWith(policy: FilingPolicy) {
        config += policy
    }

    /**
     * For static mocks: Defines a loading function to retrieve the scenario files as a stream
     * @param loading a function to load files by name and path as a stream (could use
     * Android's assets.open, Classloader.getRessourceAsStream, FileInputStream, etc.)
     */
    override fun loadFileWith(loading: FileLoader) {
        config = config.copy(openFile = loading)
    }

    /**
     * Uses dynamic mocks to answer network requests instead of file scenarios
     * @param callback A callback to invoke when a request in intercepted: must return a
     * ResponseDescriptor for the current Request or null if not suitable Response could be
     * computed
     */
    override fun useDynamicMocks(callback: RequestCallback) {
        dynamicCallbacks += callback
    }

    /**
     * Defines the mapper to use to parse the scenario files (Jackson, Moshi, GSON...)
     * @param objectMapper A Mapper to parse scenario files.
     */
    override fun parseScenariosWith(objectMapper: Mapper) {
        config = config.copy(mapper = objectMapper)
    }

    /**
     * Defines the folder where and how scenarios should be stored when recording. This method is
     * for Java compatibility. For Kotlin users, prefer the recordScenariosIn() extension.
     * @param folder the root folder where saved scenarios should be saved
     * @param policy the naming policy to use for scenario files
     */
    override fun saveScenarios(folder: File, policy: FilingPolicy?) {
        recorder = RecorderBuilder(folder, policy)
    }

    /**
     * Allows to return an error if saving fails when recording.
     * @param failOnError if true, failure to save scenarios will throw an exception.
     * If false, saving exceptions will be ignored.
     */
    override fun failOnRecordingError(failOnError: Boolean) {
        config = config.copy(showSavingErrors = failOnError)
    }

    /**
     * Allows to set a fake delay for every requests (can be overridden in a scenario) to
     * achieve a more realistic behavior (probably necessary if you want to display loading
     * animations during your network calls).
     * @param delay default pause delay for network responses in ms
     */
    override fun addFakeNetworkDelay(delay: Long) {
        config = config.copy(simulatedDelay = delay)
    }

    /**
     * Defines how the interceptor should initially behave (can be enabled, disable, record
     * requests...)
     * @param status The interceptor mode
     */
    override fun setInterceptorStatus(status: Mode) {
        config.status = status
    }

    override fun buildConfig(): Config = config.copy(providers = buildProviders())

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
        } else emptyList()
    }
}
