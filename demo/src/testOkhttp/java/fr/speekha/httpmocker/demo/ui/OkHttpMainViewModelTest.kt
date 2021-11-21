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
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.okhttp.builder.mockInterceptor
import fr.speekha.httpmocker.okhttp.builder.recordScenariosIn
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import java.io.File

@ExperimentalCoroutinesApi
class OkHttpMainViewModelTest : MainViewModelTest() {

    private val mockResponseInterceptor = mockInterceptor {
        parseScenariosWith(JacksonMapper())
        recordScenariosIn(File(""))
    }

    @Before
    fun setup() {
        mockService = mockk()
        viewModel = MainViewModel(mockService, MockerWrapper(mockResponseInterceptor))
    }

    override fun assertInterceptorMode(mode: Mode) = assertEquals(mode, mockResponseInterceptor.mode)
}
