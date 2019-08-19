/*
 * Copyright 2019 David Blanc
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
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.model.User
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import fr.speekha.httpmocker.jackson.JacksonMapper
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val mockService = mockk<GithubApiEndpoints>()
    private val mockResponseInterceptor = MockResponseInterceptor.Builder()
        .parseScenariosWith(JacksonMapper())
        .build()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        viewModel = MainViewModel(mockService, mockResponseInterceptor)
    }

    @Test
    fun `should succeed repos and top contributors calls`() = runBlockingTest {
        val observer = spyk<Observer<Data>>()
        viewModel.getData().observeForever(observer)

        coEvery { mockService.listRepositoriesForOrganisation(org) } returns
                listOf(Repo(id, repo, topContributor = contributor))
        coEvery { mockService.listContributorsForRepository(org, repo) } returns
                listOf(User(login = contributor, contributions = contributions))

        viewModel.callService()

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
    fun `should succeed repos call and fail top contributors call`() = runBlockingTest {
        val observer = spyk<Observer<Data>>()
        viewModel.getData().observeForever(observer)
        coEvery { mockService.listRepositoriesForOrganisation(org) } returns
                listOf(Repo(id, repo, topContributor = contributor))
        coEvery { mockService.listContributorsForRepository(org, repo) } throws IOException("Test exception")

        viewModel.callService()

        coVerifyOrder {
            observer.onChanged(Data.Loading)
            observer.onChanged(Data.Success(listOf(Repo(id, repo))))
        }
        confirmVerified(observer)
        viewModel.getData().removeObserver(observer)
    }

    @Test
    fun `should fail repos call`() = runBlockingTest {
        val errorMessage = "error"
        val observer = spyk<Observer<Data>>()
        viewModel.getData().observeForever(observer)
        coEvery { mockService.listRepositoriesForOrganisation(org) } throws IOException(errorMessage)

        viewModel.callService()

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

        viewModel.setMode(MockResponseInterceptor.Mode.DISABLED)

        assertEquals(mockResponseInterceptor.mode, MockResponseInterceptor.Mode.DISABLED)
        verify { observer.onChanged(State.Message(R.string.disabled_description)) }
    }

    @Test
    fun `should update state according to enabled mode`() {
        val observer = spyk<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(MockResponseInterceptor.Mode.ENABLED)

        assertEquals(mockResponseInterceptor.mode, MockResponseInterceptor.Mode.ENABLED)
        verify { observer.onChanged(State.Message(R.string.enabled_description)) }
    }

    @Test
    fun `should update state according to mixed mode`() {
        val observer = spyk<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(MockResponseInterceptor.Mode.MIXED)

        assertEquals(mockResponseInterceptor.mode, MockResponseInterceptor.Mode.MIXED)
        verify { observer.onChanged(State.Message(R.string.mixed_description)) }
    }

    @Test
    fun `should check permission and update state according to record mode`() {
        val observer = spyk<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(MockResponseInterceptor.Mode.RECORD)

        assertEquals(mockResponseInterceptor.mode, MockResponseInterceptor.Mode.RECORD)
        verify { observer.onChanged(State.Permission) }
        verify { observer.onChanged(State.Message(R.string.record_description)) }
    }
}
