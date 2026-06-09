/*
 * Copyright 2019-2026 David Blanc
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

import io.ktor.client.engine.HttpClientEngineConfig

/**
 * iOS-specific configuration. Recording to a path-based folder is available via the
 * [MockableClientConfiguration.recordScenariosIn] overload that accepts a [String] or
 * [fr.speekha.httpmocker.io.IosFileAccessor], both defined in commonMain.
 */
class IosMockableClientConfiguration<T : HttpClientEngineConfig> : MockableClientConfiguration<T>()

actual fun <T : HttpClientEngineConfig> initConfiguration(): MockableClientConfiguration<T> =
    IosMockableClientConfiguration()
