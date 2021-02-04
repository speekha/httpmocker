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

package fr.speekha.httpmocker.demo.ui

import androidx.lifecycle.Observer
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.model.User
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.okhttp.builder.mockInterceptor
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class MainViewModelTest : ViewModelTest() {

    private val org = "kotlin"
    private val repo = "repo"
    private val contributor = "contributor"
    private val contributions = 1
    private val id = 0L

    private lateinit var mockService: GithubApiEndpoints
    private val mockResponseInterceptor =
        mockInterceptor {
            parseScenariosWith(JacksonMapper())
        }

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        mockService = mockk()
        viewModel = MainViewModel(mockService, MockerWrapper(mockResponseInterceptor))
    }

    @Test
    fun `should succeed repos and top contributors calls`() {
        val observer = spyk<Observer<Data>>()
        runBlocking {
            viewModel.getData().observeForever(observer)

            coEvery { mockService.listRepositoriesForOrganisation(org) } returns
                listOf(Repo(id, repo, topContributor = contributor))
            coEvery { mockService.listContributorsForRepository(org, repo) } returns
                listOf(User(login = contributor, contributions = contributions))

            viewModel.callService().join()
        }

        coVerifyOrder {
            observer.onChanged(Data.Loading)
            observer.onChanged(
                Data.Success(
                    listOf(
                        Repo(
                            id,
                            repo,
                            topContributor = "$contributor - $contributions contributions"
                        )
                    )
                )
            )
        }
        confirmVerified(observer)
        viewModel.getData().removeObserver(observer)
    }

    @Test
    fun `should succeed repos call and fail top contributors call`() {
        val observer = spyk<Observer<Data>>()
        runBlocking {
            viewModel.getData().observeForever(observer)
            coEvery { mockService.listRepositoriesForOrganisation(org) } returns
                listOf(Repo(id, repo, topContributor = contributor))
            coEvery {
                mockService.listContributorsForRepository(
                    org,
                    repo
                )
            } throws IOException("Test exception")

            viewModel.callService().join()
        }

        coVerifyOrder {
            observer.onChanged(Data.Loading)
            observer.onChanged(Data.Success(listOf(Repo(id, repo))))
        }
        confirmVerified(observer)
        viewModel.getData().removeObserver(observer)
    }

    @Test
    fun `should fail repos call`() {
        val errorMessage = "error"
        val observer = spyk<Observer<Data>>()
        runBlocking {
            viewModel.getData().observeForever(observer)
            coEvery { mockService.listRepositoriesForOrganisation(org) } throws IOException(
                errorMessage
            )

            viewModel.callService().join()
        }

        coVerifyOrder {
            observer.onChanged(Data.Loading)
            observer.onChanged(Data.Error(errorMessage))
        }
        confirmVerified(observer)
        viewModel.getData().removeObserver(observer)
    }

    @Test
    fun `should update state according to disabled mode`() {
        val observer = spyk<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(Mode.DISABLED)

        assertEquals(mockResponseInterceptor.mode, Mode.DISABLED)
        verify { observer.onChanged(State.Message(R.string.disabled_description)) }
    }

    @Test
    fun `should update state according to enabled mode`() {
        val observer = spyk<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(Mode.ENABLED)

        assertEquals(mockResponseInterceptor.mode, Mode.ENABLED)
        verify { observer.onChanged(State.Message(R.string.enabled_description)) }
    }

    @Test
    fun `should update state according to mixed mode`() {
        val observer = spyk<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(Mode.MIXED)

        assertEquals(mockResponseInterceptor.mode, Mode.MIXED)
        verify { observer.onChanged(State.Message(R.string.mixed_description)) }
    }

    @Test
    fun `should check permission and update state according to record mode`() {
        val observer = spyk<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(Mode.RECORD)

        assertEquals(mockResponseInterceptor.mode, Mode.RECORD)
        verify { observer.onChanged(State.Permission) }
        verify { observer.onChanged(State.Message(R.string.record_description)) }
    }
}
