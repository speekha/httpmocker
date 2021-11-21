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

package fr.speekha.httpmocker.demo.ui

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.kotlinx.KotlinxMapper
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.ktor.engine.MockEngine
import io.ktor.client.engine.cio.*
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before

@ExperimentalCoroutinesApi
class KtorMainViewModelTest : MainViewModelTest() {

    private val mockClient = mockableHttpClient(CIO) {
        mock {
            parseScenariosWith(KotlinxMapper())
            recordScenariosIn("")
        }
    }

    private val clientMode: Mode
        get() = (mockClient.engine as MockEngine).mode

    @Before
    fun setup() {
        mockService = mockk()
        viewModel = MainViewModel(mockService, MockerWrapper(mockClient))
    }

    override fun assertInterceptorMode(mode: Mode) = assertEquals(mode, clientMode)
}
