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

package fr.speekha.httpmocker.ktor.engine

import fr.speekha.httpmocker.builder.ConfigBuilder
import fr.speekha.httpmocker.builder.Configurator
import fr.speekha.httpmocker.io.FileAccessor
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig

class MockEngineConfig
private constructor(internal val configBuilder: ConfigBuilder) :
    HttpClientEngineConfig(), Configurator by configBuilder {

    constructor() : this(ConfigBuilder())

    lateinit var delegate: HttpClientEngine

    val saveFolder: FileAccessor?
        get() = configBuilder.recorder?.rootFolder
}
