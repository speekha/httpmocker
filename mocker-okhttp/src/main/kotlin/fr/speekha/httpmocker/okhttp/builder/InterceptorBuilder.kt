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

package fr.speekha.httpmocker.okhttp.builder

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.NO_ROOT_FOLDER_ERROR
import fr.speekha.httpmocker.builder.ConfigBuilder
import fr.speekha.httpmocker.builder.FileLoader
import fr.speekha.httpmocker.io.FileAccessor
import fr.speekha.httpmocker.okhttp.MockResponseInterceptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.scenario.RequestCallback
import fr.speekha.httpmocker.serialization.Mapper
import java.io.File

class InterceptorBuilder {

    internal val configBuilder = ConfigBuilder()

    /**
     * For static mocks: Defines the policy used to retrieve the configuration files based
     * on the request being intercepted
     * @param policy the naming policy to use for scenario files
     */
    fun decodeScenarioPathWith(policy: FilingPolicy): InterceptorBuilder = apply {
        configBuilder.decodeScenarioPathWith(policy)
    }

    /**
     * For static mocks: Defines a loading function to retrieve the scenario files as a stream
     * @param loading a function to load files by name and path as a stream (could use
     * Android's assets.open, Classloader.getRessourceAsStream, FileInputStream, etc.)
     */
    fun loadFileWith(loading: FileLoader): InterceptorBuilder = apply {
        configBuilder.loadFileWith(loading::load)
    }

    /**
     * Uses dynamic mocks to answer network requests instead of file scenarios
     * @param callback A callback to invoke when a request in intercepted: must return a
     * ResponseDescriptor for the current Request or null if not suitable Response could be
     * computed
     */
    fun useDynamicMocks(callback: RequestCallback): InterceptorBuilder = apply {
        configBuilder.useDynamicMocks(callback)
    }

    /**
     * Defines the mapper to use to parse the scenario files (Jackson, Moshi, GSON...)
     * @param objectMapper A Mapper to parse scenario files.
     */
    fun parseScenariosWith(objectMapper: Mapper): InterceptorBuilder = apply {
        configBuilder.parseScenariosWith(objectMapper)
    }

    /**
     * Defines the folder where and how scenarios should be stored when recording. This method is
     * for Java compatibility. For Kotlin users, prefer the recordScenariosIn() extension.
     * @param folder the root folder where saved scenarios should be saved
     * @param policy the naming policy to use for scenario files
     */
    fun recordScenariosIn(folder: File, policy: FilingPolicy?): InterceptorBuilder = apply {
        configBuilder.recordScenariosIn(FileAccessor(folder), policy)
    }

    /**
     * Allows to return an error if saving fails when recording.
     * @param failOnError if true, failure to save scenarios will throw an exception.
     * If false, saving exceptions will be ignored.
     */
    fun failOnRecordingError(failOnError: Boolean): InterceptorBuilder = apply {
        configBuilder.failOnRecordingError(failOnError)
    }

    /**
     * Allows to set a fake delay for every requests (can be overridden in a scenario) to
     * achieve a more realistic behavior (probably necessary if you want to display loading
     * animations during your network calls).
     * @param delay default pause delay for network responses in ms
     */
    fun addFakeNetworkDelay(delay: Long): InterceptorBuilder = apply { configBuilder.addFakeNetworkDelay(delay) }

    /**
     * Defines how the interceptor should initially behave (can be enabled, disable, record
     * requests...)
     * @param mode The interceptor mode
     */
    fun setMode(mode: Mode): InterceptorBuilder = apply { configBuilder.setMode(mode) }

    /**
     * Builds the interceptor.
     */
    fun build(): MockResponseInterceptor = with(configBuilder.buildConfig()) {
        MockResponseInterceptor(
            providers,
            requestWriter,
            simulatedDelay
        ).apply {
            if (this@with.mode == Mode.RECORD && configBuilder.recorder?.rootFolder == null) {
                error(NO_ROOT_FOLDER_ERROR)
            }
            mode = this@with.mode
        }
    }
}
