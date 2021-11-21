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
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.model.User
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import io.mockk.coEvery
import io.uniflow.android.test.createTestObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
abstract class MainViewModelTest : ViewModelTest() {

    private val org = "kotlin"
    private val repo = "repo"
    private val contributor = "contributor"
    private val contributions = 1
    private val id = 0L

    protected lateinit var mockService: GithubApiEndpoints

    protected lateinit var viewModel: MainViewModel

    abstract fun assertInterceptorMode(mode: Mode)

    @Test
    fun `should succeed repos and top contributors calls`() {
        val observer = viewModel.createTestObserver()

        runBlocking {
            coEvery { mockService.listRepositoriesForOrganisation(org) } returns
                listOf(Repo(id, repo, topContributor = contributor))
            coEvery { mockService.listContributorsForRepository(org, repo) } returns
                listOf(User(login = contributor, contributions = contributions))

            viewModel.callService().join()
        }

        observer.verifySequence(
            State(R.string.disabled_description, Mode.DISABLED, Data.Loading),
            State(
                R.string.disabled_description,
                Mode.DISABLED,
                Data.Success(listOf(Repo(id, repo, "$contributor - $contributions contributions")))
            )
        )
    }

    @Test
    fun `should succeed repos call and fail top contributors call`() {
        val observer = viewModel.createTestObserver()
        runBlocking {
            coEvery { mockService.listRepositoriesForOrganisation(org) } returns
                listOf(Repo(id, repo, topContributor = contributor))
            coEvery {
                mockService.listContributorsForRepository(org, repo)
            } throws IOException("Test exception")

            viewModel.callService().join()
        }

        observer.verifySequence(
            State(R.string.disabled_description, Mode.DISABLED, Data.Loading),
            State(R.string.disabled_description, Mode.DISABLED, Data.Success(listOf(Repo(id, repo))))
        )
    }

    @Test
    fun `should fail repos call`() {
        val errorMessage = "error"
        val observer = viewModel.createTestObserver()
        runBlocking {
            coEvery {
                mockService.listRepositoriesForOrganisation(org)
            } throws IOException(errorMessage)

            viewModel.callService().join()
        }

        observer.verifySequence(
            State(R.string.disabled_description, Mode.DISABLED, Data.Loading),
            State(R.string.disabled_description, Mode.DISABLED, Data.Error(errorMessage))
        )
    }

    @Test
    fun `should update state according to disabled mode`() {
        val observer = viewModel.createTestObserver()

        viewModel.setMode(Mode.DISABLED)

        assertInterceptorMode(Mode.DISABLED)
        observer.verifySequence(State(R.string.disabled_description, Mode.DISABLED, Data.Empty))
    }

    @Test
    fun `should update state according to enabled mode`() {
        val observer = viewModel.createTestObserver()

        viewModel.setMode(Mode.ENABLED)

        assertInterceptorMode(Mode.ENABLED)
        observer.verifySequence(State(R.string.enabled_description, Mode.ENABLED, Data.Empty))
    }

    @Test
    fun `should update state according to mixed mode`() {
        val observer = viewModel.createTestObserver()

        viewModel.setMode(Mode.MIXED)

        assertInterceptorMode(Mode.MIXED)
        observer.verifySequence(
            State(R.string.mixed_description, Mode.MIXED, Data.Empty)
        )
    }

    @Test
    fun `should check permission and update state according to record mode`() {
        val observer = viewModel.createTestObserver()

        viewModel.setMode(Mode.RECORD)

        assertInterceptorMode(Mode.RECORD)
        observer.verifySequence(
            Permission,
            State(R.string.record_description, Mode.RECORD, Data.Empty)
        )
    }
}
