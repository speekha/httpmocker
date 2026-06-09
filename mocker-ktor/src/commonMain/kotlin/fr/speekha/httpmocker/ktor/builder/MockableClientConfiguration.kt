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

package fr.speekha.httpmocker.ktor.builder

import fr.speekha.httpmocker.builder.RecorderBuilder
import fr.speekha.httpmocker.io.FileAccessor
import fr.speekha.httpmocker.io.createFileAccessor
import fr.speekha.httpmocker.ktor.engine.MockEngineConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.defaultTransformers
import io.ktor.http.ContentType

open class MockableClientConfiguration<T : HttpClientEngineConfig> {

    private val config = HttpClientConfig<MockEngineConfig>()

    /**
     * Use [HttpRedirect] plugin to automatically follow redirects.
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

    /**
     * Defines the folder where and how scenarios should be stored when recording.
     * @param folder the root folder where saved scenarios should be saved
     */
    fun MockEngineConfig.recordScenariosIn(folder: FileAccessor): RecorderBuilder =
        RecorderBuilder(folder).also { configBuilder.recorder = it }

    /**
     * Defines the folder where and how scenarios should be stored when recording.
     * @param folder the root folder where saved scenarios should be saved
     */
    fun MockEngineConfig.recordScenariosIn(folder: String): RecorderBuilder =
        recordScenariosIn(createFileAccessor(folder))

    /**
     * Applies all the installed plugins and customInterceptors from this configuration
     * into the specified [client].
     */
    fun install(client: HttpClient) {
        config.install(client)
    }

    /**
     * Installs a specific [plugin] and optionally [configure] it.
     */
    fun <TBuilder : Any, TPlugin : Any> install(
        plugin: HttpClientPlugin<TBuilder, TPlugin>,
        configure: TBuilder.() -> Unit = {}
    ) {
        config.install(plugin, configure)
    }

    fun applyConfiguration(conf: HttpClientConfig<MockEngineConfig>) {
        conf += config
    }
}

expect fun <T : HttpClientEngineConfig> initConfiguration(): MockableClientConfiguration<T>
