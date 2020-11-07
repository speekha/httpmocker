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

package fr.speekha.httpmocker.ktor.engine

import fr.speekha.httpmocker.builder.RecorderBuilder
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.features.HttpRedirect
import io.ktor.client.features.defaultTransformers
import java.io.File

class MockableClientConfiguration<T : HttpClientEngineConfig> {

    private val config = HttpClientConfig<MockEngineConfig>()

    /**
     * Use [HttpRedirect] feature to automatically follow redirects.
     */
    var followRedirects: Boolean by config::followRedirects

    /**
     * Use [defaultTransformers] to automatically handle simple [ContentType].
     */
    var useDefaultTransformers: Boolean by config::useDefaultTransformers

    /**
     * Terminate [HttpClient.responsePipeline] if status code is not success(>=300).
     */
    var expectSuccess: Boolean by config::expectSuccess

    internal var mockConfiguration: MockEngineConfig.() -> Unit = {}

    internal var delegateConfiguration: (T) -> Unit = {}

    fun engine(block: (T) -> Unit) {
        delegateConfiguration = block
    }

    fun mock(block: MockEngineConfig.() -> Unit) {
        mockConfiguration = block
    }

    fun MockEngineConfig.recordScenariosIn(folder: File): RecorderBuilder =
        RecorderBuilder(folder).also { configBuilder.recorder = it }

    fun MockEngineConfig.recordScenariosIn(folder: String): RecorderBuilder =
        recordScenariosIn(File(folder))

    /**
     * Applies all the installed [features] and [customInterceptors] from this configuration
     * into the specified [client].
     */
    fun install(client: HttpClient) {
        config.install(client)
    }

    /**
     * Installs a specific [feature] and optionally [configure] it.
     */
    fun <TBuilder : Any, TFeature : Any> install(
        feature: HttpClientFeature<TBuilder, TFeature>,
        configure: TBuilder.() -> Unit = {}
    ) {
        config.install(feature, configure)
    }

    /**
     * Installs an interceptor defined by [block].
     * The [key] parameter is used as a unique name, that also prevents installing duplicated interceptors.
     */
    fun install(key: String, block: HttpClient.() -> Unit) {
        config.install(key, block)
    }

    fun applyConfiguration(conf: HttpClientConfig<MockEngineConfig>) {
        conf += config
    }
}
