package fr.speekha.httpmocker.demo.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.model.User
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    var coroutinesTestRule = CoroutinesTestRule()
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val org = "kotlin"
    private val repo = "repo"
    private val contributor = "contributor"
    private val contributions = 1
    private val id = 0L

    private val mockService = mock<GithubApiEndpoints>()
    private val mockResponseInterceptor =
        MockResponseInterceptor.Builder().parseScenariosWith(mock()).build()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        viewModel = MainViewModel(mockService, mockResponseInterceptor)
    }

    @Test
    fun `should load repos and top contributors successfully`() {
        val observer = mock<Observer<Data>>()
        viewModel.getData().observeForever(observer)

        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(mockService.listRepositoriesForOrganisation(org))
                .thenReturn(listOf(Repo(id, repo, topContributor = contributor)))
            whenever(mockService.listContributorsForRepository(org, repo))
                .thenReturn(listOf(User(login = contributor, contributions = contributions)))
            viewModel.callService()
        }

        verify(observer).onChanged(Data.Loading)
        verify(observer).onChanged(
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
        viewModel.getData().removeObserver(observer)
    }

    @Test
    fun `should load repos successfully and fail top contributors`() {
        val observer = mock<Observer<Data>>()
        viewModel.getData().observeForever(observer)

        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(mockService.listRepositoriesForOrganisation(org))
                .thenReturn(listOf(Repo(id, repo, topContributor = contributor)))
            whenever(mockService.listContributorsForRepository(org, repo))
                .thenReturn(null)
            viewModel.callService()
        }

        verify(observer).onChanged(Data.Loading)
        verify(observer).onChanged(
            Data.Success(
                listOf(Repo(id, repo))
            )
        )
        viewModel.getData().removeObserver(observer)
    }

    @Test
    fun `should update state according to disabled mode`() {
        val observer = mock<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(MockResponseInterceptor.Mode.DISABLED)

        verify(observer).onChanged(State.Message(R.string.disabled_description))
    }

    @Test
    fun `should update state according to enabled mode`() {
        val observer = mock<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(MockResponseInterceptor.Mode.ENABLED)

        verify(observer).onChanged(State.Message(R.string.enabled_description))
    }

    @Test
    fun `should update state according to mixed mode`() {
        val observer = mock<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(MockResponseInterceptor.Mode.MIXED)

        verify(observer).onChanged(State.Message(R.string.mixed_description))
    }

    @Test
    fun `should check permission and update state according to record mode`() {
        val observer = mock<Observer<State>>()
        viewModel.getState().observeForever(observer)

        viewModel.setMode(MockResponseInterceptor.Mode.RECORD)

        verify(observer).onChanged(State.Permission)
        verify(observer).onChanged(State.Message(R.string.record_description))
    }
}
