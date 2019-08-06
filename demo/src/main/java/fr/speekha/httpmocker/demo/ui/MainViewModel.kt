package fr.speekha.httpmocker.demo.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.model.onFailure
import fr.speekha.httpmocker.demo.model.onSuccess
import fr.speekha.httpmocker.demo.model.resultOf
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val apiService: GithubApiEndpoints,
    private val mocker: MockResponseInterceptor
) : ViewModel() {

    private val data = MutableLiveData<Data>()
    private val state = MutableLiveData<State>()

    fun getData(): LiveData<Data> = data
    fun getState(): LiveData<State> = state

    fun callService() {
        viewModelScope.launch {
            data.postValue(Data.Loading)
            val org = "kotlin"
            loadRepos(org)
                .onSuccess { repos ->
                    repos.map { repo ->
                        val contributor =
                            loadTopContributor(org, repo.name).getOrNull()?.firstOrNull()
                        repo.copy(topContributor = contributor?.run { "$login - $contributions contributions" })
                    }.also {
                        data.postValue(Data.Success(it))
                    }
                }.onFailure {
                    data.postValue(Data.Error(it.message))
                }
        }
    }

    fun setMode(mode: MockResponseInterceptor.Mode) {
        mocker.mode = mode
        if (mocker.mode == MockResponseInterceptor.Mode.RECORD) {
            state.postValue(State.Permission)
        }
        state.postValue(
            State.Message(
                when (mocker.mode) {
                    MockResponseInterceptor.Mode.DISABLED -> R.string.disabled_description
                    MockResponseInterceptor.Mode.ENABLED -> R.string.enabled_description
                    MockResponseInterceptor.Mode.MIXED -> R.string.mixed_description
                    MockResponseInterceptor.Mode.RECORD -> R.string.record_description
                }
            )
        )
    }

    private suspend fun loadRepos(org: String) = withContext(Dispatchers.IO) {
        resultOf {
            apiService.listRepositoriesForOrganisation(org)
        }
    }

    private suspend fun loadTopContributor(org: String, repo: String) =
        withContext(Dispatchers.IO) {
            resultOf {
                apiService.listContributorsForRepository(org, repo)
            }.onFailure {
                Log.e("ViewModel", it.message, it)
            }
        }
}

sealed class Data {
    object Loading : Data()
    data class Success(val repos: List<Repo>) : Data()
    data class Error(val message: String?) : Data()
}

sealed class State {
    data class Message(@StringRes val message: Int) : State()
    object Permission : State()
}
