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
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val apiService: GithubApiEndpoints,
    private val mocker: MockResponseInterceptor
) : ViewModel() {

    private val data = MutableLiveData<Data>()
    private val state = MutableLiveData<State>()
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        data.postValue(Data.Error(exception.message))
    }

    fun getData(): LiveData<Data> = data
    fun getState(): LiveData<State> = state

    fun callService() {
        data.postValue(Data.Loading)
        viewModelScope.launch(exceptionHandler) {
            val org = "kotlin"
            val repos = loadRepos(org)
                .map {
                    val contributor = loadTopContributor(org, it.name)?.firstOrNull()
                    it.copy(topContributor = contributor?.run { "$login - $contributions contributions" })
                }
            data.postValue(Data.Success(repos))
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
        apiService.listRepositoriesForOrganisation(org)
    }

    private suspend fun loadTopContributor(org: String, repo: String) =
        withContext(Dispatchers.IO) {
            try {
                apiService.listContributorsForRepository(org, repo)
            } catch (e: Throwable) {
                Log.e("Presenter", e.message, e)
                null
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
