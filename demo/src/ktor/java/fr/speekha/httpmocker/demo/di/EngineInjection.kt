/*
 *  Copyright 2019-2021 David Blanc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package fr.speekha.httpmocker.demo.di

import android.content.Context
import android.os.Environment
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import fr.speekha.httpmocker.demo.service.GithubEndpointWithKtor
import fr.speekha.httpmocker.demo.ui.MockerWrapper
import fr.speekha.httpmocker.io.asReader
import fr.speekha.httpmocker.kotlinx.KotlinxMapper
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.ktor.builder.saveScenarios
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

val engineInjection: Module = module {

    single {
        mockableHttpClient(CIO) {
            mock {
                decodeScenarioPathWith(get())
                loadFileWith { get<Context>().assets.open(it).asReader() }
                parseScenariosWith(KotlinxMapper())
                saveScenarios(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    get()
                )
                addFakeNetworkDelay(500)
            }

            install(JsonFeature) {
                serializer = KotlinxSerializer(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }

            expectSuccess = false
            followRedirects = false
        }
    }

    single<GithubApiEndpoints> { GithubEndpointWithKtor(get()) }

    single { MockerWrapper(get()) }
}
