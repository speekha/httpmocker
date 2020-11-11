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

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.NO_ROOT_FOLDER_ERROR
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.util.InternalAPI

object MockClient : HttpClientEngineFactory<MockEngineConfig> {

    @InternalAPI
    override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine {
        val config = MockEngineConfig().apply(block)
        return MockEngine(config, config.delegate::execute).apply {
            if (mode == Mode.RECORD && config.saveFolder == null) {
                error(NO_ROOT_FOLDER_ERROR)
            }
        }
    }
}
